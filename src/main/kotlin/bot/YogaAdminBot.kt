package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class YogaAdminBot {

    fun createBot(): Bot {
        return bot {
            token = "6786187377:AAGoTGeWMfW_9bKCqFFxs-MX2I5eEmbEoV0"
            timeout = 30

            dispatch {
                setUpCommands()
                setUpCallbacks()
            }
        }
    }

    private fun Dispatcher.setUpCallbacks() {

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
                    text = getNamesFromFile(), //показываем список имен в группе
                    replyMarkup = inlineKeyboardMarkup
                )
            }//конец коллбэка groupEdit

        callbackQuery(callbackData = "freezeMembership") {//коллбек freezeMembership
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val name = callbackQuery.message?.chat?.firstName ?: return@callbackQuery
            if (!getNamesFromFile().contains(name)) //если студента нет в группе
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "У вас нет абонемента :(\nЧтобы его приобрести, обратитесь к Учителю: @ebarnaeva")
            else { //если студент есть в группе
                if (getDataFromFile(name, 2).toInt() >20)
                    {
                        bot.sendMessage(chatId = ChatId.fromId(chatId),
                            text = "Заморозка возможна только один раз в течение месяца. " +
                                    "\nЧтобы её оформить, обратитесь к Учителю: @ebarnaeva")
                    } else {
                        bot.sendMessage(chatId = ChatId.fromId(chatId),
                            text = "Заморозка возможна только для безлимитных абонементов. " +
                                    "\nЧтобы перейти на безлимит, обратитесь к Учителю: @ebarnaeva")
                    }

            }
            return@callbackQuery
        }//конец коллбека freezeMembership

        var userToDelete : String  = ""
        callbackQuery(callbackData = "deleteStudent") {//коллбек deleteStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery

            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Введите телегам-имя йогини для удаления")

            message(Filter.Text) {//приём ответа от пользователя
                userToDelete = message.text.toString() //сохраняем ответ в память
                println("удаляем $userToDelete")//проверяем, что собираемся удалить
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
                text = "Ты выбрала удалить - ${userToDelete}, верно? \nЕсли неверно, введи имя йогини ещё раз.",
                replyMarkup = inlineKeyboardMarkup
            )
        }
            return@callbackQuery
        }//конец коллбека deleteStudent


        callbackQuery(callbackData = "confirmDeleting") {//коллбек confirmDeleting
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val messageFile = "src/yogaList.txt"
                if (!getNamesFromFile().contains(userToDelete)) { //проверяем имя наличие в файле
                    println("Нет такой йогини") //йогини нет в списке
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "ОШИБКА!\nНет такой йогини, проверь правильность имени")
                } else { //Йогиня есть в списке, удаляем её из файла
                        try {
                        println("Приступаем к удалению $userToDelete")

                            val lines = BufferedReader(FileReader(messageFile)).readLines()
                            println(lines) //проверяем что считалось из файла
                            // Удаление строки, начинающейся с userToDelete
                            val filteredLines = lines.filterNot { it.trimStart().startsWith(userToDelete) }
                            println(filteredLines) //проверяем что считалось получилось после удаления
                            val writer = FileWriter(messageFile)
                            filteredLines.forEach { writer.write(it + "\n") }
                            writer.close()
                        bot.sendMessage(chatId = ChatId.fromId(chatId),
                            text = "Йогиня удалена успешно.\nТеперь в твоей группе порядок!!!" +
                                    "\nДля возврата к главному меню нажми /start")
                        userToDelete = "" //обнуляем userToDelete - для исключения повторных срабатываний
                    } catch (e: IOException) {
                        e.printStackTrace()
                        println("Ошибка при удалении строки из файла.")
                    }
                }
            return@callbackQuery
        }//конец коллбека confirmDeleting

        var newUserString : String  = ""
        callbackQuery(callbackData = "addStudent") {//коллбек addStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Введи через пробел:\nТелегам-имя йогини,\nДату окончания абонемента и\nколичество оплаченных уроков")
            message(Filter.Text) {//приём ответа от пользователя
                newUserString = message.text.toString() //сохраняем ответ в память
                println("сохраняем $newUserString")//проверяем, что сохраняем в память

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
                    text = "Ты добавляешь - $newUserString, верно? \nЕсли неверно, введи имя_дату_уроки ещё раз.",
                    replyMarkup = inlineKeyboardMarkup
                )
            }
            return@callbackQuery
        }//конец коллбека addStudent

        callbackQuery(callbackData = "confirmCreating") {//коллбек confirmCreating
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val messageFile = "src/yogaList.txt"
            if (!newUserString.matches(Regex("^[^\\s]*\\s[^\\s]*\\s[^\\s]*$"))) //если строка НЕ содержит 2 пробела
            {
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "ОШИБКА В СТРОКЕ!\nВведи через пробел:\nТелегам-имя йогини,\nДату окончания абонемента и\nколичество оплаченных уроков")
            } else {//т.к. строка содержит 2 пробела, проверяем на повторное внесение
                if (getNamesFromFile().contains(newUserString.split(" ")[0])) { //проверяем имя на повтор
                    println("Повторное внесение!!!")
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "ОШИБКА В ФАЙЛЕ!\nЭта йогиня уже есть в списке!")
                } else { //Йогини нет в списке, дописываем её в файл
                    try {
                        println("В строке ровно 2 пробела: всё ОК")
                        val fileWriter = FileWriter(messageFile, true) // Открываем файл для добавления
                        val bufferedWriter =
                            BufferedWriter(fileWriter)// Используем BufferedWriter для более эффективной записи

                        bufferedWriter.write(newUserString)// Добавляем строку в файл
                        bufferedWriter.newLine() // Добавляем перенос строки
                        bufferedWriter.close()// Закрываем BufferedWriter и FileWriter
                        fileWriter.close()
                        bot.sendMessage(chatId = ChatId.fromId(chatId),
                            text = "Новая йогиня добавлена успешно.\nТвоя группа растёт, поздравляю!!!" +
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

    private fun deleteStudentFromFile(newStudentString: Message?): Boolean {
        TODO("Not yet implemented")
    }


    private fun getNamesFromFile(): String { //функция чтения имен пользователей в группе
        val messageFile = "src/yogaList.txt"  // !!Replace with the actual file name
        var names = ""
            val lines = File(messageFile).readLines() //читаем файл построчно
            // Creating an Array with names only
        for (line in lines) {
            names += line.split(" ")[0] + "\n"
        }
            //result is ready :)
    if (names.isEmpty())    names = "В группе нет учеников :( "
        println(names) //проверяем работу функции
        return names.dropLast(1) //we don't need the last "\n
    } //конец функции чтения имен пользователей в группе

    private fun getDataFromFile(name: String, column : Int): String { //функция чтения данных пользователя в группе
        val messageFile = "src/yogaList.txt"  // !!Replace with the actual file name
        val lines = File(messageFile).readLines() //читаем файл построчно
        var data : String = ""
        // Looking for a string with data
        for (line in lines) {
            if (line.split(" ")[0] == name)
            {
                data = line.split(" ")[column]
                //print("Найдено значение $data") //проверка работы с файлом
                return data //result is ready :)
            }
            else data = "не найдено"
        }
        return data //result is ready :)
    } //конец функции чтения данных пользователя в группе


    private fun Dispatcher.setUpCommands() {
        command("start") {
            // chatStates[message.chat.id] = ChatState.START
            println("1 User name: " + message.chat.firstName + " ChatID: " + message.chat.id + " MessageID: " + message.messageId + " SenderChatId: " + message.senderChat?.id)
            //проверка на Учителя "819577258" (временно меняю на свой ID)
            if (message.chat.id.toString() == "433077424")
            {
                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Изменить группу",
                            callbackData = "groupEdit"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Редактировать абонементы",
                            callbackData = "membershipEdit"
                        )
                    ),
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Подарить занятие",
                            callbackData = "giftLesson"
                        )
                    )
                )
                val chatId = message.chat.id
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Выбери команду, Учитель",
                    replyMarkup = inlineKeyboardMarkup
                )
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

                    replyMarkup = inlineKeyboardMarkup
                )
            }
        }
        //конец функции start для остальных
    }//конец Диспетчера
}//конец Бот-класса
