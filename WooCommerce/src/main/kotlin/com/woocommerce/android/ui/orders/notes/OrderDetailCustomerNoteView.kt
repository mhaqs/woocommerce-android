package com.woocommerce.android.ui.orders.notes

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_customer_note.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerNoteView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_customer_note, this)
    }

    fun initView(order: WCOrderModel) {
        customerNote_msg.text = order.customerNote
    }
}
