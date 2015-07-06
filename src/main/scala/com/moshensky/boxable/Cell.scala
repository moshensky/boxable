package com.moshensky.boxable

import java.awt.Color

import org.apache.pdfbox.pdmodel.font.{PDType1Font, PDFont}

//import be.quodlibet.boxable.{Paragraph, Row}
/**
 * Created by moshensky on 7/5/15.
 */
class Cell(row: Row, width: Float, var text: String) {
  private var font: PDFont = PDType1Font.HELVETICA
  private var fontSize: Float = 8
  private var fillColor: Color = null
  private var textColor: Color = Color.BLACK

  def getTextColor: Color = {
    return textColor
  }

  def setTextColor(textColor: Color) {
    this.textColor = textColor
  }

  def getFillColor: Color = {
    return fillColor
  }

  def setFillColor(fillColor: Color) {
    this.fillColor = fillColor
  }

  def getWidth: Float = {
    return width
  }

  def getText: String = {
    return text
  }

  def setText(text: String) {
    this.text = if (text == null) "" else text
  }

  def getFont: PDFont = {
    if (font == null) {
      throw new IllegalArgumentException("Font not set.")
    }
    return font
  }

  def setFont(font: PDFont) {
    this.font = font
  }

  def getFontSize: Float = {
    return fontSize
  }

  def setFontSize(fontSize: Float) {
    this.fontSize = fontSize
  }

  def getParagraph: Paragraph = {
    new Paragraph(text, font, fontSize.toInt, width.toInt)
  }

  def getExtraWidth: Float = {
    this.row.getLastCellExtraWidth + getWidth
  }

  def getHeight: Float = {
    row.getHeight
  }
}

object Cell {
  /**
   *
   * @param width in % of table width
   * @param text
   */
  def apply(row: Row, width: Float, text: String, isCalculated: Boolean) = {
    val cellWidth = if (isCalculated) {
      val calclulatedWidth: Double = (row.getWidth * width) / 100
      calclulatedWidth.toFloat
    }
    else {
      width
    }

    if (cellWidth > row.getWidth) {
      throw new IllegalArgumentException("Cell Width=" + cellWidth + " can't be bigger than row width=" + row.getWidth)
    }

    val cellText = if (text == null) "" else text

    new Cell(row, width, text)
  }
}
