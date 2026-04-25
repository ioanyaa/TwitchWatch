class MainActivity : ComponentActivity() {
    private var chatClient: WearChatClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // înlocuiește cu IP-ul telefonului tău (vezi Settings > About > IP)
        chatClient = WearChatClient("10.41.92.128")
        chatClient?.connect()

        setContent {
            TwitchWatchTheme {
                AppScaffold {
                    ChatScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatClient?.disconnect()
    }
}