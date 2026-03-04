package xyz.quaver.pupil.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Animatable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.github.piasy.biv.view.BigImageView
import com.github.piasy.biv.view.ImageShownCallback
import com.github.piasy.biv.view.ImageViewFactory
import kotlinx.coroutines.*
import xyz.quaver.pupil.R
import xyz.quaver.pupil.databinding.ReaderItemBinding
import xyz.quaver.pupil.hitomi.GalleryInfo
import xyz.quaver.pupil.ui.ReaderActivity
import xyz.quaver.pupil.util.downloader.Cache
import java.io.File
import kotlin.math.roundToInt

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLEncoder
import java.net.URL

class ReaderAdapter(
    private val activity: ReaderActivity,
    private val galleryID: Int
) : RecyclerView.Adapter<ReaderAdapter.ViewHolder>() {

    var galleryInfo: GalleryInfo? = null
    var isFullScreen = false
    var onItemClickListener : (() -> (Unit))? = null

    private var cache: Cache? = null

    inner class ViewHolder(private val binding: ReaderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {

            binding.image.setOnClickListener {
                onItemClickListener?.invoke()
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke()
            }
        }

        fun bind(position: Int) {

            if (cache == null)
                cache = Cache.getInstance(itemView.context, galleryID)

            binding.readerIndex.text = (position+1).toString()

            val image = cache!!.getImage(position)
            val progress = activity.downloader?.progress?.get(galleryID)?.get(position)

            if (progress?.isInfinite() == true && image != null) {

                binding.progressGroup.visibility = View.INVISIBLE

                binding.image.showImage(image.uri)

                // AUTO TRANSLATE
                translateImage(image)

            } else {

                binding.progressGroup.visibility = View.VISIBLE

                binding.readerItemProgressbar.progress =
                    if (progress?.isInfinite() == true)
                        100
                    else
                        progress?.roundToInt() ?: 0

                CoroutineScope(Dispatchers.Main).launch {

                    delay(1000)

                    notifyItemChanged(position)

                }
            }

        }

        fun clear() {

            binding.image.mainView.let {

                when (it) {

                    is SubsamplingScaleImageView ->
                        it.recycle()

                    is SimpleDraweeView ->
                        it.controller = null

                }

            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(

            ReaderItemBinding.inflate(

                LayoutInflater.from(parent.context),

                parent,

                false

            )

        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(position)

    }

    override fun getItemCount() = galleryInfo?.files?.size ?: 0

    override fun onViewRecycled(holder: ViewHolder) {

        holder.clear()

    }

    // ===============================
    // AUTO TRANSLATE FUNCTION
    // ===============================

    private fun translateImage(file: File) {

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        val image = InputImage.fromBitmap(bitmap,0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->

                val text = result.text

                if(text.isNotEmpty()){

                    CoroutineScope(Dispatchers.IO).launch {

                        val url =
"https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=id&dt=t&q=" +
URLEncoder.encode(text,"UTF-8")

                        val translated = URL(url).readText()

                        println("TRANSLATED = $translated")

                    }

                }

            }

    }

}
