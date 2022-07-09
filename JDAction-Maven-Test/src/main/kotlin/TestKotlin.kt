import net.dv8tion.jda.api.JDABuilder

fun testFileFacade() {
    val jda = JDABuilder.createLight("")
        .build()
        .awaitReady()

    jda.retrieveUserById(0)
}

class TestKotlin {
    fun testNormalClass() {
        val jda = JDABuilder.createLight("")
            .build()
            .awaitReady()

        jda.retrieveUserById(0)
    }

    inner class Inner {
        fun testInnerClass() {
            val jda = JDABuilder.createLight("")
                .build()
                .awaitReady()

            jda.retrieveUserById(0)
        }
    }
}