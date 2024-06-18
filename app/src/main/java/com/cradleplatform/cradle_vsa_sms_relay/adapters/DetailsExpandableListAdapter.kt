package com.cradleplatform.cradle_vsa_sms_relay.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.TextView
import com.cradleplatform.cradle_vsa_sms_relay.R

class DetailsExpandableListAdapter(private val context: Context,
                                   private val groupList: List<String>,
                                   private val childList: HashMap<String, List<Map<String, String>>>
)
    : BaseExpandableListAdapter() {
    override fun getGroupCount(): Int {
        return this.groupList.size
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.childList[groupList[listPosition]]?.size ?: 0
    }

    override fun getGroup(listPosition: Int): Any {
        return this.groupList[listPosition]
    }

    override fun getChild(listPosition: Int, expandableListPosition: Int): Any {
        return this.childList[this.groupList[listPosition]]?.get(expandableListPosition) ?:
        "Group does not exist"
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getChildId(listPosition: Int, expandableListPosition: Int): Long {
        return expandableListPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
       return false
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?,
                              parent: ViewGroup?): View {
        var convertView = convertView
        if(convertView == null){
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.details_list_group, null)
        }
        val expandableListView = parent as ExpandableListView
        if(isExpanded) expandableListView.expandGroup(listPosition)
        val listTitle = getGroup(listPosition) as String
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.listTitle)
        listTitleTextView.text = listTitle
        return convertView
    }

    override fun getChildView(listPosition: Int, expandableListPosition: Int, isLastChild: Boolean,
                              convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        if(convertView == null){
            val layoutInflater =
                this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.details_list_item, null)
        }
        val childDict =  getChild(listPosition, expandableListPosition) as Map<String,String>
        var expandedListText = childDict.entries.joinToString(separator = "\n") { "${it.key}:" +
                " ${it.value}" }
        if (expandedListText.isNullOrBlank()) {expandedListText = "No content to show"}
        val expandedListTextView = convertView!!.findViewById<TextView>(R.id.expandedListItemText)
        expandedListTextView.text = expandedListText
        return convertView

    }

    override fun isChildSelectable(listPosition: Int, p1: Int): Boolean {
        return true
    }
}
