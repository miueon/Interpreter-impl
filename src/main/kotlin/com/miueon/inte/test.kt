package com.miueon.inte

class A{
    companion object{
        val keywords:MutableMap<String, String> = HashMap()
        init {
            keywords["and"] = "AND"
        }
    }
}
fun main() {
    print(A.keywords["and"])
}