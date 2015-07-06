package com.moshensky.boxable

import org.apache.pdfbox.pdmodel.font.{PDType1Font, PDFont}

/**
 * Created by moshensky on 7/5/15.
 */
class Paragraph {
  private var width: Int = 500
  private var text: String = null
  private var font: PDFont = PDType1Font.HELVETICA
  private var fontSize: Int = 10
  private var color: Int = 0

  def this(text: String, font: PDFont, fontSize: Int, width: Int) {
    this()
    this.text = text
    this.font = font
    this.fontSize = fontSize
    this.width = width
  }

  def getLines: List[String] = {
    val split: Array[String] = text.split("(?<=\\s|-|@|,|\\.|:|;)")
    val splitedTextPartsLength = split.map(_.length)
    val possibleWrapPoints = splitedTextPartsLength.drop(1).foldLeft(List(splitedTextPartsLength.head))((list, splitLength) => {
      (splitLength + list.head) :: list
    })

    var start: Int = 0
    var end: Int = 0
    var result = List[String]()
    possibleWrapPoints.foreach(pwp => {
      val width = font.getStringWidth(text.substring(start, pwp)) / 1000 * fontSize

      if (start < end && width > this.width) {
        result = text.substring(start, end)::result
        start = end
      }

      end = pwp
    })

    text.substring(start)::result
  }

  def getFontHeight: Float = {
    return font.getFontDescriptor.getFontBoundingBox.getHeight / 1000 * fontSize
  }

  def getFontWidth: Float = {
    return font.getFontDescriptor.getFontBoundingBox.getWidth / 1000 * fontSize
  }

  def withWidth(width: Int): Paragraph = {
    this.width = width
    return this
  }

  def withFont(font: PDFont, fontSize: Int): Paragraph = {
    this.font = font
    this.fontSize = fontSize
    return this
  }

  def withColor(color: Int): Paragraph = {
    this.color = color
    return this
  }

  def getColor: Int = {
    return color
  }

  def getWidth: Int = {
    return width
  }

  def getText: String = {
    return text
  }

  def getFont: PDFont = {
    return font
  }

  def getFontSize: Int = {
    return fontSize
  }
}
