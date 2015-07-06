package com.moshensky.boxable

import java.io.File

import org.apache.pdfbox.pdmodel.font.{PDType0Font, PDFont}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.{PDPageContentStream, PDDocument, PDPage}
import org.log4s._
import sun.reflect.generics.reflectiveObjects.NotImplementedException

//import be.quodlibet.boxable.{Cell, BoxableUtils, Row}
/**
 * Created by moshensky on 7/5/15.
 */
abstract class Table[T <: PDPage](var currentPage: T) {
  private[this] val logger = getLogger

  private var document: PDDocument = null
  private var margin: Float = 0
  private var tableContentStream: PDPageContentStream = null
  private var bookmarks: List[PDOutlineItem] = null
  private val VerticalCellMargin: Float = 2f
  private val HorizontalCellMargin: Float = 2f
  private val xSpacing: Int = 0
  private var header: Row = null
  private var rows: List[Row] = List()
  private var yStartNewPage: Float = 0
  private var yStart: Float = 0
  private var bottomMargin: Float = 0
  private val topMargin: Float = 10
  private var width: Float = 0
  private var drawLines: Boolean = false
  private var drawContent: Boolean = false

  def this(yStartNewPage: Float, bottomMargin: Float, width: Float, margin: Float, document: PDDocument, drawLines: Boolean, drawContent: Boolean, currentPage: T, yStart: Float) {
    this(currentPage)
    this.document = document
    this.drawLines = drawLines
    this.drawContent = drawContent
    this.yStartNewPage = yStartNewPage
    this.margin = margin
    this.width = width
    this.yStart = yStart + topMargin
    this.bottomMargin = bottomMargin
    loadFonts
    this.yStart = yStart
    this.tableContentStream = createPdPageContentStream
  }

  def this(yStartNewPage: Float, bottomMargin: Float, width: Float, margin: Float, document: PDDocument, drawLines: Boolean, drawContent: Boolean) {
    this(createPage)
    this.document = document
    this.drawLines = drawLines
    this.drawContent = drawContent
    this.yStartNewPage = yStartNewPage
    this.margin = margin
    this.width = width
    this.bottomMargin = bottomMargin
    loadFonts
    this.tableContentStream = createPdPageContentStream
  }

  protected def loadFonts

  protected def loadFont(fontPath: String): PDFont = {
    val barcodeFontFile = new File(fontPath)
    PDType0Font.load(document, barcodeFontFile)
  }

  protected def getDocument: PDDocument = {
    document
  }

  def drawTitle(title: String, font: PDFont, fontSize: Int) {
    drawTitle(title, font, fontSize, null)
  }

  def drawTitle(title: String, font: PDFont, fontSize: Int, textType: TextType) {
    val articleTitle = createPdPageContentStream
    articleTitle.beginText
    articleTitle.setFont(font, fontSize)
    articleTitle.moveTextPositionByAmount(getMargin, yStart)
    articleTitle.setNonStrokingColor(Color.black)
    articleTitle.drawString(title)
    articleTitle.endText

    if (textType != null) {
      textType match {
        case HIGHLIGHT =>
          throw new NotImplementedException
        case SQUIGGLY =>
          throw new NotImplementedException
        case STRIKEOUT =>
          throw new NotImplementedException
        case UNDERLINE =>
          val y: Float = (yStart - 1.5).toFloat
          val titleWidth: Float = font.getStringWidth(title) / 1000 * fontSize
          articleTitle.drawLine(getMargin, y, getMargin + titleWidth, y)
        case _ =>
      }
    }

    articleTitle.close
    yStart = (yStart - (fontSize / 1.5)).toFloat
  }

  def getWidth: Float = {
    width
  }

  def createRow(height: Float): Row = {
    val row: Row = new Row(this, height)
    this.rows.add(row)
    return row
  }

  def createRow(cells: List[Cell], height: Float): Row = {
    val row: Row = new Row(this, cells, height)
    this.rows.add(row)
    return row
  }

  @throws(classOf[IOException])
  def draw: Float = {
    import scala.collection.JavaConversions._
    for (row <- rows) {
      drawRow(row)
    }
    endTable
    return yStart
  }

  @throws(classOf[IOException])
  private def drawRow(row: Row) {
    var bookmark: Boolean = false
    if (row.getBookmark != null) {
      bookmark = true
      val bookmarkDestination: PDPageXYZDestination = new PDPageXYZDestination
      bookmarkDestination.setPage(currentPage)
      bookmarkDestination.setTop(yStart.toInt)
      row.getBookmark.setDestination(bookmarkDestination)
      this.addBookmark(row.getBookmark)
    }
    if (isEndOfPage(row)) {
      endTable
      this.yStart = yStartNewPage
      this.currentPage = createPage
      this.tableContentStream = createPdPageContentStream
      if (header != null) {
        drawRow(header)
      }
      else {
        LOGGER.warn("No Header Row Defined.")
      }
    }
    if (drawLines) {
      drawVerticalLines(row)
    }
    if (drawContent) {
      drawCellContent(row)
    }
  }

  private def createPdPageContentStream: PDPageContentStream = {
    new PDPageContentStream(getDocument, getCurrentPage, true, true)
  }

  private def drawCellContent(row: Row) {
    var nextX: Float = margin + HorizontalCellMargin
    val nextY: Float = yStart - (row.getLineHeight - VerticalCellMargin)
    import scala.collection.JavaConversions._
    for (cell <- row.getCells) {
      if (cell.getFont == null) {
        throw new IllegalArgumentException("Font is null on Cell=" + cell.getText)
      }
      this.tableContentStream.setFont(cell.getFont, cell.getFontSize)
      this.tableContentStream.setNonStrokingColor(cell.getTextColor)
      this.tableContentStream.beginText
      this.tableContentStream.moveTextPositionByAmount(nextX, nextY)
      val lines: List[String] = cell.getParagraph.getLines
      var numLines: Int = cell.getParagraph.getLines.size
      this.tableContentStream.appendRawCommands(cell.getParagraph.getFontHeight + " TL\n")
      import scala.collection.JavaConversions._
      for (line <- cell.getParagraph.getLines) {
        this.tableContentStream.drawString(line.trim)
        if (numLines > 0) this.tableContentStream.appendRawCommands("T*\n")
        numLines -= 1
      }
      this.tableContentStream.endText
      this.tableContentStream.closeSubPath
      nextX += cell.getWidth
    }
    yStart = yStart - row.getHeight
  }

  @throws(classOf[IOException])
  private def drawVerticalLines(row: Row) {
    var xStart: Float = margin
    val xEnd: Float = row.xEnd + xSpacing
    drawLine("Row Upper Border ", xStart, yStart, xEnd, yStart)
    val cellIterator: Iterator[Cell] = row.getCells.iterator
    while (cellIterator.hasNext) {
      val cell: Cell = cellIterator.next
      fillCellColor(cell, yStart, xStart, cellIterator)
      val yEnd: Float = yStart - row.getHeight
      drawLine("Cell Separator ", xStart, yStart, xStart, yEnd)
      xStart += getWidth(cell, cellIterator)
    }
    val yEnd: Float = yStart - row.getHeight
    drawLine("Last Cell ", xEnd, yStart, xEnd, yEnd)
  }

  @throws(classOf[IOException])
  private def drawLine(`type`: String, xStart: Float, yStart: Float, xEnd: Float, yEnd: Float) {
    this.tableContentStream.setNonStrokingColor(Color.BLACK)
    this.tableContentStream.setStrokingColor(Color.BLACK)
    this.tableContentStream.drawLine(xStart, yStart, xEnd, yEnd)
    this.tableContentStream.closeSubPath
  }

  @throws(classOf[IOException])
  private def fillCellColor(cell: Cell, yStart: Float, xStart: Float, cellIterator: Iterator[Cell]) {
    if (cell.getFillColor != null) {
      this.tableContentStream.setNonStrokingColor(cell.getFillColor)
      yStart = yStart - cell.getHeight
      val height: Float = cell.getHeight - 1f
      val width: Float = getWidth(cell, cellIterator)
      this.tableContentStream.fillRect(xStart, yStart, width + xSpacing, height)
      this.tableContentStream.closeSubPath
      this.tableContentStream.setNonStrokingColor(Color.BLACK)
    }
  }

  private def getWidth(cell: Cell, cellIterator: Iterator[Cell]): Float = {
    var width: Float = .0
    if (cellIterator.hasNext) {
      width = cell.getWidth
    }
    else {
      width = cell.getExtraWidth
    }
    return width
  }

  protected def createPage: T

  @throws(classOf[IOException])
  private def endTable {
    if (drawLines) {
      drawLine("Row Bottom Border ", this.margin, this.yStart, this.margin + width + xSpacing, this.yStart)
    }
    this.tableContentStream.close
  }

  def getCurrentPage: T = {
    checkNotNull(this.currentPage, "No current page defined.")
    return this.currentPage
  }

  def isEndOfPage(row: Row): Boolean = {
    val currentY: Float = yStart - row.getHeight
    val isEndOfPage: Boolean = currentY <= (bottomMargin + 10)
    return isEndOfPage
  }

  private def addBookmark(bookmark: PDOutlineItem) {
    if (bookmarks == null) bookmarks = new ArrayList[_]
    bookmarks.add(bookmark)
  }

  def getBookmarks: List[PDOutlineItem] = {
    return bookmarks
  }

  def setHeader(header: Row) {
    this.header = header
  }

  def getHeader: Row = {
    if (header == null) {
      throw new IllegalArgumentException("Header Row not set on table")
    }
    return header
  }

  private[boxable] def getMargin: Float = {
    return margin
  }

  protected def setYStart(yStart: Float) {
    this.yStart = yStart
  }
}
