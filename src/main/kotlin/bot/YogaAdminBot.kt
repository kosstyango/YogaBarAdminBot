package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import java.io.File

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

        callbackQuery(callbackData = "myMembership") {//коллбек myMembership
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val name = callbackQuery.message?.chat?.firstName ?: return@callbackQuery
            if (!getNamesFromFile().contains(name)) //если студента нет в группе
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "У вас нет абонемента :(\nЧтобы его приобрести, обратитесь к Учителю: @ebarnaeva")
            else{ //если студент есть в группе
                if  (getDataFromFile(name, 2).toInt()>20) //если у студента безлимитный абонемент
                {
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Ваш абонемент действует до ${getDataFromFile(name, 1)}. " +
                                "У Вас безлимитный тариф :)")
                }else // если у студента абонемент лимитный
                    bot.sendMessage(chatId = ChatId.fromId(chatId),
                        text = "Ваш абонемент действует до ${getDataFromFile(name, 1)}, " +
                                "доступно ${getDataFromFile(name, 2)} занятий")
            }
            return@callbackQuery
        }//конец коллбека myMembership

        callbackQuery(callbackData = "deleteStudent") {//коллбек deleteStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
                bot.sendMessage(chatId = ChatId.fromId(chatId),
                    text = "Введите телегам-имя студента для удаления")
                    deleteStudentFromFile()
                return@callbackQuery
        }//конец коллбека deleteStudent

        callbackQuery(callbackData = "addStudent") {//коллбек addStudent
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId = ChatId.fromId(chatId),
                text = "Введите телегам-имя студента, дату активации его абонемента и количество оплаченных уроков")
            // надо принять строку для работы с файлом
            return@callbackQuery
        }//конец коллбека addStudent

        callbackQuery(callbackData = "restart") {//коллбек restart
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Для начала работы введите /start")
            return@callbackQuery
        }//конец коллбека restart
//
//        message(Filter.Text) {
//            val chatId = message.chat.id
//            if (chatStates[chatId] != ChatState.MANUAL_LOCATION) {
//                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Для начала работы введите start")
//                return@message
//            }
//            val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
//                listOf(
//                    InlineKeyboardButton.CallbackData(
//                        text = "Да, верно.",
//                        callbackData = "yes_label"
//                    )
//                )
//            )
//            countries[chatId] = message.text.toString()
//            bot.sendMessage(
//                chatId = ChatId.fromId(chatId),
//                text = "Твой город - ${message.text}, верно? \nЕсли неверно, введи название города ещё раз.",
//                replyMarkup = inlineKeyboardMarkup
//            )
//        }
//
//        callbackQuery(callbackData = "enterManually") {
//            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
//            if (chatStates[chatId] != ChatState.START) {
//                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Для начала работы введите start")
//                return@callbackQuery
//            }
//            chatStates[chatId] = ChatState.MANUAL_LOCATION
//            bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Хорошо, введи название города:")
//
//        }
//
//        callbackQuery(callbackData = "yes_label") {
//            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
//            if (chatStates[chatId] != ChatState.AUTO_LOCATION&&chatStates[chatId] != ChatState.MANUAL_LOCATION) {
//                bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Для начала работы введите start")
//                return@callbackQuery
//            }
//            bot.apply {
//                sendAnimation(chatId = ChatId.fromId(chatId), animation = TelegramFile.ByUrl(GIF_WAITING_URL))
//                sendMessage(chatId = ChatId.fromId(chatId), text = "Узнаём погоду в городе ${countries[chatId]}...")
//                sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)
//            }
//            CoroutineScope(Dispatchers.IO).launch {//запускаем Корутину
//                val windDirection: String
//                val chatId =
//                    callbackQuery.message?.chat?.id ?: return@launch //здесь зависает, если город введён сошибкой
//                val currentWeather : CurrentWeather
//                try{
//                    currentWeather = weatherRepository.getCurrentWeather(//Корутина пошла узнавать погоду
//                        apiKey = WEATHER_API_KEY,
//                        queryCountry = countries[chatId] ?: "",
//                        isAqiNeeded = "no"
//                    )}catch(e:Exception){
//                    e.printStackTrace()
//                    bot.sendMessage(chatId = ChatId.fromId(chatId), text = "Такого города не знаю. Повторите попытку")
//                    return@launch
//                }
//
//                }
//                bot.sendMessage(//Корутина отправляет сообщение с погодой
//                    chatId = ChatId.fromId(chatId),
//                    text = """
//                            Сейчас в городе ${countries[chatId]} погода такая:
//                            ☁ Облачность: ${currentWeather.current.cloud}%
//                            🌡 Температура: ${"%.0f".format(currentWeather.current.tempDegrees)} градусов Цельсия
//                            💧 Влажность: ${currentWeather.current.humidity} %
//                            🌪 Ветер: ${currentWeather.current.windKph} км/час ${windDirection}
//                            🧭 Давление: ${"%.1f".format(25.4 * currentWeather.current.pressureIn)} мм
//                    """.trimIndent()
//                )
//                bot.sendMessage(//Корутина отправляет второе сообщение
//                    chatId = ChatId.fromId(chatId),
//                    text = "Если хочешь запросить погоду ещё раз, \nвоспользуйся командой /weather"
//                )
//            }//Конец Корутины
//        }//Конец Коллбэка Yes-label
    }//Конец Диспетчера всех Коллбеков

    private fun deleteStudentFromFile() {
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
            if (message.chat.id.toString() == "819577258") //проверка на Учителя
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
