package com.moshensky.boxable

import java.io.IOException

import org.apache.pdfbox.pdmodel.PDDocument

/**
 * Created by moshensky on 7/5/15.
 */
abstract class AbstractTemplatedTable[T: AbstractPageTemplate] extends Table[T] {
  def this(yStart: Float, yStartNewPage: Float, bottomMargin: Float, width: Float, margin: Float, document: PDDocument, currentPage: T, drawLines: Boolean, drawContent: Boolean) {
    this()
    `super`(yStart, yStartNewPage, bottomMargin, width, margin, document, currentPage, drawLines, drawContent)
  }

  def this(yStartNewPage: Float, bottomMargin: Float, width: Float, margin: Float, document: PDDocument, drawLines: Boolean, drawContent: Boolean) {
    this()
    `super`(yStartNewPage, bottomMargin, width, margin, document, drawLines, drawContent)
    setYStart(getCurrentPage.yStart)
  }
}
