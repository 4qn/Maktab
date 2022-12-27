package com.edu.maktab.model

import android.os.Parcel
import android.os.Parcelable

data class Book(
    var id: String,
    val Book_Name: String,
    val Book_Category: String,
    val Author: String,
    val Book_Language: String,
    val Number_On_Book: String,
    val Book_Rack: String,
    val Publisher: String = "",
    val Pages: Int = 0,
    val Price: Float = 0f,
    val Description: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readString().toString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(Book_Name)
        parcel.writeString(Book_Category)
        parcel.writeString(Author)
        parcel.writeString(Book_Language)
        parcel.writeString(Number_On_Book)
        parcel.writeString(Book_Rack)
        parcel.writeString(Publisher)
        parcel.writeInt(Pages)
        parcel.writeFloat(Price)
        parcel.writeString(Description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book {
            return Book(parcel)
        }

        override fun newArray(size: Int): Array<Book?> {
            return arrayOfNulls(size)
        }
    }
}
