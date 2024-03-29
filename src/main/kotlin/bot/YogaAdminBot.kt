package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class YogaAdminBot {
    val botUsers = mutableMapOf<Long, String>() //создаём карту для хранения пользователей и их chatID
    val lessonConfirms = mutableSetOf<String>() //создаём набор для хранения пользователей, кто подтвердил свой приход
    val messageFile = "src/yogaList.txt" //путь к файлу с данными
    val file = File(messageFile)
    var lastProcessedMessageId: Long = 0
    var nameToGift = ""
    lateinit var namesToCount: Array<String>

    fun createBot(): Bot {
        return bot {
            token = "6786187377:AAGoTGeWMfW_9bKCqFFxs-MX2I5eEmbEoV0"
            timeout = 30
            // Добавление значений в карту для приглашений на тренировку
            botUsers[513452246L] = "TatianaEmpathy"
            botUsers[6807894410L] = "ОльгаАракчеева"
            botUsers[1358731501L] = "Antoninatrue"
            botUsers[320480020L] = "KolotyukLubov"
            botUsers[940460912L] = "Antonina_Korablina"
            botUsers[227790650L] = "Kate"
            botUsers[1340340105L] = "wveronik"
            botUsers[661960705L] = "inna_monte"
            botUsers[240043550L] = "nastya_pianykh"
            botUsers[387174886L] = "ОльгаМафия"
            botUsers[1229738863L] = "Sokolova_Gz"
            botUsers[702640753L] = "Maryna_Stepanova"
            botUsers[5193993330L] = "Alena"

            dispatch {
                setUpCommands()
                setUpCallbacks()
            }
        }
    }

    private fun Dispatcher.setUpCallbacks() {
        //confirmCount
        callbackQuery(callbackData = "confirmCount") {//коллбек confirmCount
            println("ДО списания занятий группа выглядит так:\n${file.readText()}")
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val fileContent = file.readText()
            if (!namesToCount.all { word -> fileContent.contains(word) }) //если НЕ все слова из списка содержатся в файле
            {
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "ОШИБКА: Не могу найти этих йогинь в списке :(\nИх занятия НЕ списаны"
                )
                return@callbackQuery
            }else {
                for (userName in namesToCount) { //списываем одно занятие из абонемента каждого, кто пришёл БЕЗ записи
                    //println("пробуем удалить занятия у йогини $userName")
                    try {
                        val lines = FileReader(messageFile).readLines()
                        //println("Построчный файл yogaList: $lines")
                        var lessons = getDataFromFile(userName, 2).toInt()
                        lessons-- //убавление занятий
                        // Изменение строки, начинающейся с имени зарегистрированного на урок
                        val updatedLines = lines.map { line ->
                            if (line.startsWith(userName)) {
                                //println("Берём строку $line:")
                                val words = line.split("\\s+".toRegex())
                                if (words.size >= 3) {
                                    // Замена третьего слова
                                    val updatedLine =
                                        words.subList(0, 2) + listOf(lessons) + words.subList(3, words.size)
                                    updatedLine.joinToString(" ")
                                } else {
                                    line // Строка слишком короткая, пропустить замену
                                }
                            } else {
                                line // Строка НЕ начинается с имени явившегося без записи, пропустить замену
                            }
                        }
                        file.writeText(updatedLines.joinToString("\n"))// Запись обновленного содержимого обратно в файл
                    } catch (e: IOException) {
                        e.printStackTrace()
                        println("Ошибка при работе с файлом.")
                    }
                    println("ПОСЛЕ списания занятий группа выглядит так: \n${file.readText()}")
                }
                bot.sendMessage( //отправляем сообщение Учителю
                    chatId = ChatId.fromId(chatId),
                    text = "Сегодня пришли без записи: ${namesToCount.toList()} \n" +
                            "Абонементы йогинь без записи успешно обновлены.\n")
                for (userName in namesToCount){ //извещаем йогинь о списании оплаченных уроков
                    if (getDataFromFile(userName, 3) != null) { //если у йогини указан номер чата
                        val newChatId : Long = getDataFromFile(userName, 3).toLong()
                        bot.sendMessage( //отправляем сообщение Йогиням
                            chatId = ChatId.fromId(newChatId),
                            text = "Спасибо за практику, $userName, мы сегодня отлично позанимались!!!" +
                                    "\nПожалуйста, записывайся на практику заранее." +
                                    "\nТвой абонемент обновлён :)")
                    }
                }
            } //конец else
            return@callbackQuery
        }//конец коллбека confirmCount

//surpriseLessons
callbackQuery(callbackData = "surpriseLessons") {//коллбек surpriseLessons
            println("ДО убавления уроков группа выглядит так: ${file.readText()}")
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            for (userName in lessonConfirms) { //убавляем одно занятие из абонемента каждого, кто записался на урок
                println("пробуем убавить 1 урок у йогини $userName")
                try {
                    val lines = FileReader(messageFile).readLines()
                    println("Построчный файл yogaList: $lines")
                    var lessons = getDataFromFile(userName, 2).toInt()
                    lessons-- //убавление занятий
                    // Изменение строки, начинающейся с имени зарегистрированного на урок
                    val updatedLines = lines.map { line ->
                        if (line.startsWith(userName)) {
                            println("Берём строку $line:")
                            val words = line.split("\\s+".toRegex())
                            if (words.size >= 3) {
                                // Замена третьего слова
                                val updatedLine = words.subList(0, 2) + listOf(lessons) + words.subList(3, words.size)
                                updatedLine.joinToString(" ")
                            } else {
                                line // Строка слишком короткая, пропустить замену
                            }
                        } else {
                            line // Строка НЕ начинается с имени зарегистрированного на урок, пропустить замену
                        }
                    }
                    file.writeText(updatedLines.joinToString("\n"))// Запись обновленного содержимого обратно в файл
                } catch (e: IOException) {
                    e.printStackTrace()
                    println("Ошибка при работе с файлом.")
                }
                    //println("после убавления уроков группа выглядит так: ${file.readText()}")
            }
            println("На сегодня записывались: ${lessonConfirms.toString()} \nИх абонементы уже обновлены")
            bot.sendMessage( //отправляем сообщение Учителю
               chatId = ChatId.fromId(chatId),
               text = "На сегодня записывались: $lessonConfirms \n" +
                       "Их абонементы уже обновлены.\n" +
                       "Внеси ЧЕРЕЗ ПРОБЕЛ имена йогинь, которые пришли БЕЗ> записи:")

    for (userName in lessonConfirms){ //извещаем йогинь об убавлении оплаченных уроков
        if (getDataFromFile(userName, 3) != null) { //если у йогини указан номер чата
            val newChatId : Long = getDataFromFile(userName, 3).toLong()
            bot.sendMessage( //отправляем сообщение Йогиням
                chatId = ChatId.fromId(newChatId),
                text = "Спасибо за предварительную запись, $userName, мы сегодня отлично позанимались!!!" +
                        "\nВыкладывай фотки в Instagramm с хэштегом #yogabarclub, отмечай @yoga_freshbar." +
                        "\nТвой абонемент обновлён :)")
            }
    }
    lessonConfirms.clear() //обнуляем список записавшихся, т.к. уже убавили им оплаченные занятия

    message(Filter.Text) {//приём ответа от пользователя
        val messageId = callbackQuery.message?.messageId ?: return@message
        lastProcessedMessageId = callbackQuery.message?.messageId!! //сохраняем номер обрабатываемого сообщения
        if (messageId < lastProcessedMessageId) return@message //попытка предотвратить повторные обработки ответов
        namesToCount = message.text.toString().split(" ").toTypedArray()
        println("убавляем уроки у ${namesToCount.toSet()}")//проверяем, кого собираемся посчитать
        val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
            listOf(
                InlineKeyboardButton.CallbackData(
                    text = "Да, верно.",
                    callbackData = "confirmCount"
                )
            )
        )
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = "Без записи пришли ${namesToCount.toList()}, верно? \nЕсли неверно, введи имена йогинь ещё раз.",
            replyMarkup = inlineKeyboardMarkup)
        return@message //пробуем прекратить многократную обработку сообщений
    }
           return@callbackQuery
        }//конец коллбека surpriseLessons

//refuseLesson
        callbackQuery(callbackData = "refuseLesson") {//коллбек refuseLesson
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            //если пользователь скрыл свой username, мы его будем идентифицировать по его firstName
            val name = callbackQuery.message?.chat?.username ?: callbackQuery.message?.chat?.firstName
            //а если он и firstName как-то скрыл, то идентифиццировать его будем по номеру чата
            ?: callbackQuery.message?.chat?.id.toString()
            println("Фиксируем отказ от пользователя $name из чата $chatId")
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Понял. Но ты знай:\nнам тебя будет не хватать :(")
            return@callbackQuery
        }//конец коллбека refuseLesson

//confirmLesson
        callbackQuery(callbackData = "confirmLesson") {//коллбек confirmLesson
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            //если пользователь скрыл свой username, мы его будем идентифицировать по его firstName
            val name = callbackQuery.message?.chat?.username ?: callbackQuery.message?.chat?.firstName
            //а если он и firstName как-то скрыл, то идентифиццировать его будем по номеру чата
            ?: callbackQuery.message?.chat?.id.toString()
            println("Записываем подтверждение от пользователя $name из чата $chatId")
            lessonConfirms.add(name) //добавляем пользователя в список на завтра
            println(lessonConfirms.toString()) //проверяем, что у нас уже есть в списке
                bot.sendMessage(
                    chatId = ChatId.fromId(819577258), // извещаем Учителя
                    text = "На тренировку записалась йогиня $name.")
            println(lessonConfirms.toString()) //проверяем, что у нас уже есть в списке
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId), // подтверждаем запись йогине
                    text = "Супер!!! До встречи на коврике завтра!")
            return@callbackQuery
        }//конец коллбека confirmLesson

        callbackQuery(callbackData = "lessonInvite") {//коллбек lessonInvite
            lessonConfirms.clear() //обнуляем список
            println("Начинаем цикл извещений пользователей из карты")
            //println("Пока в списке: ${lessonConfirms.toString()}")
            for ((chatId, userName) in botUsers) {
                //println("Извещаем йогиню $userName...") //проверяем, кому отправляем извещение
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Конечно, да!!!",
                            callbackData = "confirmLesson"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Нет, завтра не смогу :(",
                            callbackData = "refuseLesson"
                        )
                    )
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Привет, моя йогиня $userName, \nзавтра у нас тренировка. \nТы придёшь?",
                    replyMarkup = inlineKeyboardMarkup)

            }
            return@callbackQuery
        }//конец коллбека lessonInvite

        callbackQuery(callbackData = "groupEdit") { //редактирование группы Учителем
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Удалить Йогиню",
                        callbackData = "deleteStudent"
                    )
                ),
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Добавить Йогиню",
                        callbackData = "addStudent"
                    )
                ),
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Back to Menu",
                        callbackData = "restart"
                    )
                )
            )
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Что выбираешь, Учитель?",
                replyMarkup = inlineKeyboardMarkup)
        }//конец коллбэка groupEdit

        callbackQuery(callbackData = "myMembership") {//коллбек myMembership
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            //если пользователь скрыл свой username, мы его будем идентифицировать по его firstName
            val name = callbackQuery.message?.chat?.username ?: callbackQuery.message?.chat?.firstName
            //а если он и firstName как-то скрыл, то идентифиццировать его будем по номеру чата
            ?: callbackQuery.message?.chat?.id.toString()
            println("Йогиня $name запросила инфо о своём абонементе")
            if (!getNamesFromFile().contains(name)) //если студента нет в группе
            {
                println("В группе НЕТ йогини с именем $name")
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "У вас нет абонемента :(\nЧтобы его приобрести, обратитесь к Учителю: @ebarnaeva")
            } else { //если студент есть в группе
                if (getDataFromFile(name, 2).toInt() > 20)//если у студента безлимит
                {
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Твой абонемент действует до ${getDataFromFile(name, 1)}" +
                                "\nБЕЗЛИМИТНЫЙ тариф - правильный выбор!" +
                                "\nДля возврата в меню нажми /start")
                } else {//если у студента НЕ безлимит
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Абонемент действует до ${getDataFromFile(name, 1)}" +
                                "\nОсталось ${getDataFromFile(name, 2)} оплаченных уроков." +
                                "\nДля возврата в меню нажми /start")
                }
            }
            return@callbackQuery
        }//конец коллбека myMembership

        callbackQuery(callbackData = "freezeMembership") {//коллбек freezeMembership
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            //если пользователь скрыл свой username, мы его будем идентифицировать по его firstName
            val name = callbackQuery.message?.chat?.username ?: callbackQuery.message?.chat?.firstName
            //а если он и firstName как-то скрыл, то идентифиццировать его будем по номеру чата
            ?: callbackQuery.message?.chat?.id.toString()
            if (!getNamesFromFile().contains(name)) //если студента нет в группе
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "У вас нет абонемента :(\nЧтобы его приобрести, обратитесь к Учителю: @ebarnaeva" +
                            "\nДля возврата в меню нажми /start")
            else { //если студент есть в группе
                if (getDataFromFile(name, 2).toInt() > 20) //Если у студента БЕЗЛИМИТ
                {
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Заморозка возможна только один раз в течение месяца. " +
                                "\nЧтобы её оформить, обратитесь к Учителю: @ebarnaeva" +
                                "\nДля возврата в меню нажми /start")
                } else { // Когда у студента простой абонемент
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Заморозка возможна ТОЛЬКО для безлимитных абонементов. " +
                                "\nЧтобы перейти на безлимит, обратитесь к Учителю: @ebarnaeva" +
                                "\nДля возврата в меню нажми /start")
                }
            }
            return@callbackQuery
        }//конец коллбека freezeMembership


        callbackQuery(callbackData = "giftLesson") {//коллбек giftLesson
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            lastProcessedMessageId = callbackQuery.message?.messageId!! //сохраняем номер обрабатываемого сообщения
            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Кого одариваем?")

            message(Filter.Text) {//приём ответа от Учителя
                val messageId = callbackQuery.message?.messageId ?: return@message
                //println("Callback giftLesson проверяет номер сообщения: ${messageId} меньше ${lastProcessedMessageId}?")
                if (messageId < lastProcessedMessageId) return@message //попытка предотвратить повторные обработки ответов
                //println("Callback giftLesson отвечает на сообщение № ${messageId}")

                nameToGift = message.text.toString()
                println("одариваем $nameToGift")//проверяем, что собираемся одарить
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Да, верно.",
                            callbackData = "confirmGift"
                        )
                    )
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Ты решила одарить - $nameToGift, верно? \nЕсли неверно, введи имя йогини ещё раз.",
                    replyMarkup = inlineKeyboardMarkup)
                return@message //пробуем прекратить многократную обработку сообщений
            }
            return@callbackQuery
        }//конец коллбека giftLesson


        callbackQuery(callbackData = "confirmGift") {//коллбек confirmGift
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery

            if (!getNamesFromFile().contains(nameToGift)) { //проверяем имя наличие в файле
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "ОШИБКА!\nНет такой йогини, проверь правильность имени")
            } else { //Йогиня есть в списке, начинаем её одаривать
                try {
                    val lines = FileReader(messageFile).readLines()
                    var lessons = getDataFromFile(nameToGift, 2).toInt()
                    lessons++ //добавление урока
                    // Изменение строки, начинающейся с name
                    val updatedLines = lines.map { line ->
                        if (line.startsWith(nameToGift)) {
                            val words = line.split("\\s+".toRegex())
                            if (words.size >= 3) {
                                // Замена третьего слова
                                val updatedLine = words.subList(0, 2) + listOf(lessons) + words.subList(3, words.size)
                                updatedLine.joinToString(" ")
                            } else {
                                line // Строка слишком короткая, пропустить замену
                            }
                        } else {
                            line // Строка не начинается с nameToGift, пропустить замену
                        }
                    }
                    file.writeText(updatedLines.joinToString("\n"))// Запись обновленного содержимого обратно в файл
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Йогиня $nameToGift одарена успешно.\nТвоя карма улучшена!!!" +
                                "\nДля возврата в меню нажми /start")
                    } catch (e: IOException) {
                    e.printStackTrace()
                    println("Ошибка при работе с файлом.")
                }
            }
            if (getDataFromFile(nameToGift, 3) != null)
            {
                println("Йогиня $nameToGift имеет адрес ${getDataFromFile(nameToGift, 3)} и может быть извещена")
                val newChatId : Long = getDataFromFile(nameToGift, 3).toLong()
                bot.sendMessage(chatId = ChatId.fromId(newChatId),
                    text = "Привет, $nameToGift! \nУчитель подарил тебе бесплатный Урок, поздравляю!!")
            }
            return@callbackQuery
        }//конец коллбека confirmGift

        var nameToDelete = ""
        callbackQuery(callbackData = "deleteStudent") {//коллбек deleteStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            lastProcessedMessageId = callbackQuery.message?.messageId!! //сохраняем номер обрабатываемого сообщения
            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Введите телегам-имя йогини для удаления")

            message(Filter.Text) {//приём ответа от пользователя
                val messageId = callbackQuery.message?.messageId ?: return@message

                //println("Callback deleteStudent проверяет номер сообщения: ${messageId} меньше ${lastProcessedMessageId}?")
                if (messageId < lastProcessedMessageId) return@message //попытка предотвратить повторные обработки ответов
                //println("Callback deleteStudent отвечает на сообщение № ${messageId}")

                nameToDelete = message.text.toString()
                //println("удаляем $nameToDelete")//проверяем, что собираемся удалить
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Да, верно.",
                            callbackData = "confirmDeleting"
                        )
                    )
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Ты выбрала удалить - $nameToDelete, верно? \nЕсли неверно, введи имя йогини ещё раз.",
                    replyMarkup = inlineKeyboardMarkup
                )
                return@message //пробуем прекратить многократную обработку сообщений
            }
            return@callbackQuery
        }//конец коллбека deleteStudent


        callbackQuery(callbackData = "confirmDeleting") {//коллбек confirmDeleting
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val messageFile = "src/yogaList.txt"
            //println("Имя к проверке: $nameToDelete")
            //println("Список: ${getNamesFromFile()}")

            if (!getNamesFromFile().contains(nameToDelete)) { //проверяем имя наличие в файле
                println("Нет такой йогини") //йогини нет в списке
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "ОШИБКА!\nНет такой йогини, проверь правильность имени")
            } else { //Йогиня есть в списке, удаляем её из файла
                try {
                    val lines = BufferedReader(FileReader(messageFile)).readLines()
                    // Удаление строки, начинающейся с name
                    val filteredLines = lines.filterNot { it.trimStart().startsWith(nameToDelete) }
                    //println(filteredLines) //проверяем что считалось получилось после удаления
                    val writer = FileWriter(messageFile)
                    filteredLines.forEach { writer.write(it + "\n") }
                    writer.close()
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Йогиня удалена успешно.\nТеперь в твоей группе порядок!!!" +
                                "\nДля возврата к главному меню нажми /start")
                } catch (e: IOException) {
                    e.printStackTrace()
                    println("Ошибка при удалении строки из файла.")
                }
            }
            return@callbackQuery
        }//конец коллбека confirmDeleting

        var newUserString = ""
        callbackQuery(callbackData = "addStudent") {//коллбек addStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            lastProcessedMessageId = callbackQuery.message?.messageId!! //сохраняем номер обрабатываемого сообщения
            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Введи через пробел:\nТелегам-имя йогини," +
                        "\nДату окончания абонемента и Количество оплаченных уроков")
            message(Filter.Text) {//приём ответа от пользователя
                val messageId = callbackQuery.message?.messageId ?: return@message
                //println("Callback addStudent отвечает на сообщение № ${messageId}")
                if (messageId < lastProcessedMessageId) return@message //попытка предотвратить повторные обработки ответов

                newUserString = message.text.toString() //сохраняем ответ в память
                //println("сохраняем $newUserString")//проверяем, что сохраняем в память

                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Да, верно.",
                            callbackData = "confirmCreating"
                        )
                    )
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Ты добавляешь $newUserString, верно? \nЕсли неверно, введи имя_дату_уроки ещё раз.",
                    replyMarkup = inlineKeyboardMarkup
                )
                return@message //пробуем прекратить многократную обработку сообщений
            } //конец функции message
            return@callbackQuery
        }//конец коллбека addStudent

        callbackQuery(callbackData = "confirmCreating") {//коллбек confirmCreating
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val messageFile = "src/yogaList.txt"
            if (!newUserString.matches(Regex("^[^\\s]*\\s[^\\s]*\\s[^\\s]*$"))) //если строка НЕ содержит 2 пробела
            {
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "ОШИБКА В СТРОКЕ!\nВведи ЧЕРЕЗ ПРОБЕЛ:\nТелегам-имя йогини,\nДату окончания абонемента и" +
                            "\nКоличество оплаченных уроков")
            } else {//т.к. строка содержит 2 пробела, проверяем на повторное внесение
                if (getNamesFromFile().contains(newUserString.split(" ")[0])) { //проверяем имя на повтор
                    println("Повторное внесение!!!")
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "ОШИБКА В ФАЙЛЕ!\nЭта йогиня уже есть в списке!")
                } else { //Йогини нет в списке, дописываем её в файл
                    try {
                        //println("В строке ровно 2 пробела: всё ОК")
                        val fileWriter = FileWriter(messageFile, true) // Открываем файл для добавления
                        val bufferedWriter =
                            BufferedWriter(fileWriter)// Используем BufferedWriter для более эффективной записи

                        bufferedWriter.write(newUserString)// Добавляем строку в файл
                        bufferedWriter.newLine() // Добавляем перенос строки
                        bufferedWriter.close()// Закрываем BufferedWriter и FileWriter
                        fileWriter.close()
                        bot.sendMessage(chatId = ChatId.fromId(chatId),
                            text = "Новая йогиня - ${newUserString.split(" ", limit = 2).firstOrNull()} " +
                                    "- добавлена успешно." +
                                    "\nТвоя группа растёт, поздравляю!!!" +
                                    "\nДля возврата к главному меню нажми /start")
                        newUserString = "" //обнуляем newUserString - для исключения повторных срабатываний
                    } catch (e: IOException) {
                        e.printStackTrace()
                        println("Ошибка при добавлении строки в файл.")
                    }
                }
            }
            return@callbackQuery
        }//конец коллбека confirmCreating

        callbackQuery(callbackData = "restart") {//коллбек restart
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Для начала работы введите /start")
            return@callbackQuery
        }//конец коллбека restart
    }//Конец Диспетчера всех Коллбеков

    //тут начинаются функции

    private fun getNamesFromFile(): String { //функция чтения имен пользователей в группе
        val messageFile = "src/yogaList.txt"  // !!Replace with the actual file name
        var names = ""
        val lines = File(messageFile).readLines() //читаем файл построчно
        // Creating an Array with names only
        for (line in lines) {
            names += line.split(" ")[0] + "\n"
        }
        //result is ready :)
        if (names.isEmpty()) names = "В группе нет учеников :( "
        //println(names) //проверяем работу функции
        return names.dropLast(1) //we don't need the last "\n
    } //конец функции чтения имен пользователей в группе

    private fun getDataFromFile(name: String, column: Int): String { //функция чтения данных пользователя в группе
        val messageFile = "src/yogaList.txt"  // !!Replace with the actual file name
        val lines = File(messageFile).readLines() //читаем файл построчно
        var data: String = ""
        // Looking for a string with data
        for (line in lines) {
            if (line.split(" ")[0] == name) {
                data = line.split(" ")[column]
                //print("Найдено значение $data") //проверка работы с файлом
                return data //result is ready :)
            } else data = "не найдено"
        }
        return data //result is ready :)
    } //конец функции чтения данных пользователя в группе


    private fun Dispatcher.setUpCommands() {

        command("start") {
            println("UserName: " + message.chat.username + " FirstName: " + message.chat.firstName + " ChatID: " + message.chat.id +
                    " MessageID: " + message.messageId)

            botUsers[message.chat.id] = message.chat.username.toString() //запоминаем всех, кто зашёл в бот
            println(botUsers.toString()) //проверяем, как работает карта

            //проверка на Учителя "819577258" (для тестирования меняю на свой ID 433077424)
            if (message.chat.id.toString() == "819577258") {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Изменить группу",
                            callbackData = "groupEdit"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Посещения без записи",
                            callbackData = "surpriseLessons"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Подарить занятие",
                            callbackData = "giftLesson"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Пригласить всех на урок",
                            callbackData = "lessonInvite"
                        )
                    )
                )
                val chatId = message.chat.id
                val messageFile = "src/yogaList.txt"
                //var groupString = File(messageFile).readText()
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Твоя группа: \n${File(messageFile).readText()}" + //выводим список группы с абонементами
                            "\nВыбери команду, Учитель",
                    replyMarkup = inlineKeyboardMarkup)
            }//конец функции start для Учителя
            else { //начало функции start для остальных
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Мой абонемент",
                            callbackData = "myMembership"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Заморозить",
                            callbackData = "freezeMembership"
                        )
                    )
                )
                val chatId = message.chat.id
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = """
                        YogaBar Практики
                        в "Status Gym", Бечичи

                        ПОНЕДЕЛЬНИК
                        10:00-11:00 - Здоровая спина, дыхание
                        19:30-20:30 - Хатха-йога на всё тело

                        СРЕДА
                        10:00-11:00 - Здоровье ТБС, растяжка
                        19:30-20:30 - Хатха-йога на всё тело

                        ПЯТНИЦА 
                        10:00-11:00 - Балансы, мобильность
                        
                        по всем вопросам пиши Учителю: @ebarnaeva
                        
                        Выбери команду, Йогиня:
                    """.trimIndent(),

                    replyMarkup = inlineKeyboardMarkup)
            }//конец функции start для йогинь
        }//конец функции start для всех
    }//конец Диспетчера
}//конец Бот-класса
