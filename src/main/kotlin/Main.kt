import bot.YogaAdminBot

fun main() {
    val bot = YogaAdminBot().createBot()
    bot.startPolling()
}