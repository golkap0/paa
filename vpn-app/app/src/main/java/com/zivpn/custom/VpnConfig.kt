package com.zivpn.custom

data class VpnConfig(
    var authUser: String = "",
    var serverHost: String = "",
    var obfsKey: String = "",
    var cores: Int = 4,
    var upLimit: String = "1 Mbps",
    var downLimit: String = "1 Mbps",
    var dns: String = "8.8.8.8,1.1.1.1",
    var recvWinConn: Int = 262144,
    var recvWin: Int = 4194304
)
