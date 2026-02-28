package com.example.myapplication

import java.util.ArrayList

class ChatGPTRespond {
    var id: String? = null
    var `object`: String? = null
    var created = 0
    var model: String? = null
    var choices: ArrayList<Choice>? = null
    var usage: Usage? = null

    inner class Choice {
        var text: String? = null
        var index = 0
        var logprobs: Any? = null
        var finish_reason: String? = null
    }

    inner class Usage {
        var prompt_tokens = 0
        var completion_tokens = 0
        var total_tokens = 0
    }
}
