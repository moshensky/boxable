package com.moshensky.boxable

import org.apache.pdfbox.pdmodel.graphics.image.{PDImageXObject}
import org.apache.pdfbox.pdmodel.{PDPageContentStream, PDPage, PDDocument}

/**
 * Created by moshensky on 7/5/15.
 */
abstract class AbstractPageTemplate extends PDPage {
  protected def getDocument: PDDocument

  protected def yStart: Float

  def addPicture(image: PDImageXObject, cursorX: Float, cursorY: Float, width: Int, height: Int) {
    val contentStream = new PDPageContentStream(getDocument, this, true, false)
    contentStream.drawImage(image, cursorX, cursorY, width, height)
    contentStream.close
  }

  def loadPicture(fileName: String): PDImageXObject = {
    PDImageXObject.createFromFile(fileName, getDocument)
  }
}
