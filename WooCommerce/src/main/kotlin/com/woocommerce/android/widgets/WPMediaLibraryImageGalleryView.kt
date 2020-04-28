package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import kotlinx.android.synthetic.main.wpmedia_gallery_item.view.*
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import java.util.ArrayList
import java.util.Locale

/**
 * Custom recycler which displays images from the WP media library
 */
class WPMediaLibraryImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    companion object {
        const val NUM_COLUMNS = 4
        private const val SCALE_NORMAL = 1.0f
        private const val SCALE_SELECTED = .8f
    }

    interface OnWPMediaGalleryClickListener {
        fun onRequestLoadMore()
    }

    private var imageSize = 0
    private val selectedIds = ArrayList<Long>()

    private val adapter: WPMediaLibraryGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: OnWPMediaGalleryClickListener

    init {
        layoutManager = GridLayoutManager(context, NUM_COLUMNS)
        itemAnimator = DefaultItemAnimator()
        layoutInflater = LayoutInflater.from(context)

        setHasFixedSize(true)
        setItemViewCacheSize(0)

        adapter = WPMediaLibraryGalleryAdapter().also {
            it.setHasStableIds(true)
            setAdapter(it)
        }

        // cancel pending Glide request when a view is recycled
        val glideRequests = GlideApp.with(this)
        setRecyclerListener { holder ->
            glideRequests.clear((holder as WPMediaViewHolder).imageView)
        }

        // create a reusable Glide request for all images
        glideRequest = glideRequests
                .asDrawable()
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())

        // create a reusable Glide rounded corner transformation for all images
        val borderRadius = context.resources.getDimensionPixelSize(R.dimen.corner_radius_small)
        glideTransform = RequestOptions.bitmapTransform(RoundedCorners(borderRadius))

        val screenWidth = DisplayUtils.getDisplayPixelWidth(context)
        val margin = context.resources.getDimensionPixelSize(R.dimen.minor_25)
        imageSize = (screenWidth / NUM_COLUMNS) - (margin * NUM_COLUMNS)
    }

    fun showImages(images: List<Product.Image>, listener: OnWPMediaGalleryClickListener) {
        this.listener = listener
        adapter.showImages(images)
    }

    private inner class WPMediaLibraryGalleryAdapter : RecyclerView.Adapter<WPMediaViewHolder>() {
        private val imageList = mutableListOf<Product.Image>()

        fun showImages(images: List<Product.Image>) {
            if (isSameImageList(images)) {
                return
            }
            imageList.clear()
            imageList.addAll(images)
            notifyDataSetChanged()
        }

        private fun isSameImageList(images: List<Product.Image>): Boolean {
            if (images.size != imageList.size) {
                return false
            }

            for (index in images.indices) {
                if (images[index].id != imageList[index].id) {
                    return false
                }
            }
            return true
        }

        fun getImage(position: Int) = imageList[position]

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WPMediaViewHolder {
            return WPMediaViewHolder(
                    layoutInflater.inflate(R.layout.wpmedia_gallery_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: WPMediaViewHolder, position: Int) {
            val image = getImage(position)
            val photonUrl = PhotonUtils.getPhotonImageUrl(image.source, 0, imageSize)
            glideRequest.load(photonUrl).apply(glideTransform).into(holder.imageView)

            val isSelected = isItemSelected(image.id)
            holder.textSelectionCount.isSelected = isSelected
            if (isSelected) {
                val count = selectedIds.indexOf(image.id) + 1
                holder.textSelectionCount.text = String.format(Locale.getDefault(), "%d", count)
            } else {
                holder.textSelectionCount.text = null
            }

            // make sure the thumbnail scale reflects its selection state
            val scale: Float = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            if (holder.imageView.scaleX != scale) {
                holder.imageView.scaleX = scale
                holder.imageView.scaleY = scale
            }

            if (position == itemCount - 1) {
                listener.onRequestLoadMore()
            }
        }

        private fun isItemSelected(imageId: Long): Boolean {
            return selectedIds.contains(imageId)
        }

        fun toggleItemSelected(holder: WPMediaViewHolder, position: Int) {
            val isSelected = isItemSelectedByPosition(position)
            setItemSelectedByPosition(holder, position, !isSelected)
        }

        private fun isItemSelectedByPosition(position: Int): Boolean {
            return isItemSelected(imageList.get(position).id)
        }

        private fun setItemSelectedByPosition(
            holder: WPMediaViewHolder,
            position: Int,
            selected: Boolean
        ) {
            val imageId = imageList.get(position).id
            if (selected) {
                selectedIds.add(imageId)
            } else {
                selectedIds.remove(imageId)
            }

            // show and animate the count
            if (selected) {
                holder.textSelectionCount
                        .setText(
                                String.format(
                                        Locale.getDefault(),
                                        "%d",
                                        selectedIds.indexOf(imageId) + 1
                                )
                        )
            } else {
                holder.textSelectionCount.setText(null)
            }
            WooAnimUtils.pop(holder.textSelectionCount)
            holder.textSelectionCount.setVisibility(if (selected) View.VISIBLE else View.GONE)

            // scale the thumbnail
            if (selected) {
                WooAnimUtils.scale(
                        holder.imageView,
                        SCALE_NORMAL,
                        SCALE_SELECTED,
                        Duration.SHORT
                )
            } else {
                WooAnimUtils.scale(
                        holder.imageView,
                        SCALE_SELECTED,
                        SCALE_NORMAL,
                        Duration.SHORT
                )
            }

            // redraw after the scale animation completes
            val delayMs: Long = Duration.SHORT.toMillis(context)
            Handler().postDelayed({ notifyDataSetChanged() }, delayMs)
            // TODO mCallback.onAdapterSelectionCountChanged(mSelectedItems.size)
        }
    }

    private inner class WPMediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: BorderedImageView = view.imageView
        val textSelectionCount: TextView = view.textSelectionCount

        init {
            imageView.layoutParams.height = imageSize
            imageView.layoutParams.width = imageSize

            itemView.setOnClickListener {
                if (adapterPosition > NO_POSITION) {
                    adapter.toggleItemSelected(this, adapterPosition)
                }
            }
        }
    }
}