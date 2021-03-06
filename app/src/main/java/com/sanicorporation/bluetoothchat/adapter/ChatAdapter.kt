package com.sanicorporation.bluetoothchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sanicorporation.bluetoothchat.R
import com.sanicorporation.bluetoothchat.model.From
import com.sanicorporation.bluetoothchat.model.Message

class ChatAdapter: RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        const val MY_MESSAGE_CONSTANT: Int = 1
        const val OTHER_MESSAGE_CONSTANT: Int = 2
    }

    private var messages: ArrayList<Message> = arrayListOf()


    override fun getItemViewType(position: Int): Int {
        return if (messages[position].from.equals(From.OTHER))
            OTHER_MESSAGE_CONSTANT
        else
            MY_MESSAGE_CONSTANT
    }

    fun getLastPosition() = messages.size-1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View
        if (viewType == MY_MESSAGE_CONSTANT){
             view = inflater.inflate(R.layout.my_message_layout,parent, false)
        } else {
                view = inflater.inflate(R.layout.other_message_layout,parent, false)
        }

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.message.text = messages[position].message
    }

    override fun getItemCount(): Int  = messages.size

    inner class MessageViewHolder(view: View): RecyclerView.ViewHolder(view){
        val message = view.findViewById<TextView>(R.id.message_text)
    }

    fun addMessage(message:Message){
        messages.add(message)
        notifyDataSetChanged()

    }
}