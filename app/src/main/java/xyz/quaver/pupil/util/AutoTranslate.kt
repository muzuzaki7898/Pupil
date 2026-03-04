object AutoTranslate {

    fun readText(bitmap: Bitmap, callback:(String)->Unit){

        val image = InputImage.fromBitmap(bitmap,0)

        val recognizer = TextRecognition.getClient()

        recognizer.process(image)
            .addOnSuccessListener {

                callback(it.text)

            }

    }

}
fun translate(text:String,callback:(String)->Unit){

    Thread{

        val url =
"https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=id&dt=t&q=$text"

        val result = URL(url).readText()

        callback(result)

    }.start()

}
