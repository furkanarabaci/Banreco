package com.illegaldisease.banreco.ocrstuff

import java.util.*

class OcrHandler(private var graphicsList : Set<OcrGraphic>){
    private val stringList : MutableList<String> = ArrayList()
    private val monthList : List<String> = ArrayList(listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"))
    private var day : Int = -1 //1-30
    private var month : Int = -1 //0 = january, 11 = december etc..
    private var year : Int = -1 //4 numbered character.
    private var hour : Int = -1 //1-60
    private var minute : Int = -1 //1-60
    init{
        splitThings()
    }
    private fun splitThings(){
        //Try to add more regex with trial and error.
        graphicsList.forEach  {
            it.textBlock.toString().split("\\s+ | -+ | /+").forEach {
                stringList.add(it) //Regex will probably not work.
            }

        }
    }
    private fun tryToParse(){ //Very ugly code, i will refactor later. aand other hilarious jokes you tell on yourself...
        stringList.forEach {
            try {
                if(day == -1) day = parseDay(it)
                if(month == -1) month = parseMonth(it)
                if(year == -1) year = parseYear(it)
                if(hour == -1) hour = parseHour(it)
                if(minute == -1) minute = parseMinute(it)
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
    fun getRenderedDate(){ //Don't call this function too much, the load is high.
        tryToParse()
        val values : MutableList<Int?> = ArrayList(listOf(day,month,year,hour,minute)).toMutableList()

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