package com.illegaldisease.banreco.ocrstuff

import android.util.Log
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class OcrHandler(private var graphicsList : Set<OcrGraphic>){
    companion object {
        val stringList : MutableList<String> = ArrayList()
        var day : Int = -1 //1-30
        var month : Int = -1 //0 = january, 11 = december etc..
        var year : Int = -1 //4 numbered character.
        var hour : Int = -1 //1-60
        var minute : Int = -1 //1-60
        @JvmStatic
        fun getRenderedDate() : String{ //Don't call this unless you called tryToParse() somewhere else before.
            var string = ""
            string += if(day < 10) "0$day" else "$day"
            string += " "
            string += if(month < 10) "0$month" else "$month"
            string += " "
            string += "$year" //Should always be 4 characters, otherwise -1.
            string += " "
            string += if(hour < 10) "0$hour" else "$hour"
            string += ":"
            string += if(minute < 10) "0$minute" else "$minute"
            return string
        }
    }

    private val monthList : List<String> = ArrayList(listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"))

    init{
        splitThings()
    }
    private fun splitThings(){
        //Try to add more regex with trial and error.
        val pattern : Pattern = Pattern.compile("[0-9]+|[A-Za-z]+")
        graphicsList.forEach  {
            val matcher : Matcher = pattern.matcher(it.textBlock.value)
            while(matcher.find()){
                stringList.add(matcher.group())
                Log.d("whatever",matcher.group())
            }

        }
    }
    fun tryToParse(){
        stringList.forEach {
            try {
                when {
                    day == -1 -> day = parseDay(it)
                    month == -1 -> month = parseMonth(it)
                    year == -1 -> year = parseYear(it)
                    hour == -1 -> hour = parseHour(it)
                    minute == -1 -> minute = parseMinute(it)
                }
                //If returnedValue could not be parsed, it will go to catch and continue.
            }
            catch (e : NumberFormatException){
                return@forEach //This is the same as continue.
            }
        }
    }
    @Throws(NumberFormatException::class)
    private fun parseDay(string : String) : Int{
        //TODO: Always control if it is max 2 character number and between 1-30
        try{
            return parseNumbers(string,1,2,1,30)
        }
        catch (e : NumberFormatException){
            throw NumberFormatException(e.message) //It is like volleyball, we redirected our try catch.....
        }
    }
    @Throws(NumberFormatException::class)
    private fun parseMonth(string : String) : Int{
        monthList.forEachIndexed { index, s ->
            if(s.toLowerCase() == string.toLowerCase()){
                return index //If we find month, how lucky. Just fork it over.
            }
        }
        try{
            return parseNumbers(string,1,2,1,12)
        }
        catch (e : NumberFormatException){
            throw NumberFormatException(e.message) //It is like volleyball, we redirected our try catch.....
        }
    }
    @Throws(NumberFormatException::class)
    private fun parseYear(string : String) : Int{
        try{
            return parseNumbers(string,4,4,1000,9999)
        }
        catch (e : NumberFormatException){
            throw NumberFormatException(e.message) //It is like volleyball, we redirected our try catch.....
        }
    }
    @Throws(NumberFormatException::class)
    private fun parseHour(string : String) : Int{
        try{
            return parseNumbers(string,1,2,0,60)
        }
        catch (e : NumberFormatException){
            throw NumberFormatException(e.message) //It is like volleyball, we redirected our try catch.....
        }
    }
    @Throws(NumberFormatException::class)
    private fun parseMinute(string : String) : Int{
        try{
            return parseNumbers(string,1,2,0,60)
        }
        catch (e : NumberFormatException){
            throw NumberFormatException(e.message) //It is like volleyball, we redirected our try catch.....
        }
    }


    /**
     * Returns -1 if requirements are not met. Throws exception if content is not a number at all.
     */
    @Throws(NumberFormatException::class)
    private fun parseNumbers(content : String, minCharacters : Int, maxCharacters : Int, minNumber : Int, maxNumber : Int) : Int{
        return try{
            if(content.length in minCharacters..maxCharacters){
                val numberAsInt = content.toInt()
                if(numberAsInt in minNumber..maxNumber){
                    numberAsInt
                } else -1
            } else{
                -1
            }
        }
        catch (e : NumberFormatException){
            throw NumberFormatException("It could not become an integer.")
        }
    }
}