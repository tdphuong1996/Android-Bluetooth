package com.nsc9012.bluetooth.extension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

fun ViewGroup.inflate(layoutRes: Int): View = LayoutInflater.from(context).inflate(
    layoutRes,
    this,
    false
)
fun ViewGroup.visible() { this.visibility = View.VISIBLE }

fun ViewGroup.invisible() { this.visibility = View.GONE }