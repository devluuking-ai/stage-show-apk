package com.devluuking.stageshow

object NodeBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun startNodeWithArguments(arguments: Array<String>): Int
}
