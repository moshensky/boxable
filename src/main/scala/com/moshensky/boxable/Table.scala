package com.moshensky.boxable

import java.awt.Color
import java.io.File

import org.apache.pdfbox.pdmodel.font.{PDType0Font, PDFont}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.{PDPageContentStream, PDDocument, PDPage}
//import org.log4s._
import sun.reflect.generics.reflectiveObjects.NotImplementedException

//import be.quodlibet.boxable.{Cell, BoxableUtils, Row}
/**
 * Created by moshensky on 7/5/15.
 */
trait TableTrait {
  def getHeader: Row
  def getWidth: Float
  def getMargin: Float
}

abstract class Table[T <: PDPage](var currentPage: T, val document: PDDocument) extends TableTrait {
  //private[this] val logger = getLogger

  private var margin: Float = 0
  private var tableContentStream: PDPageContentStream = null
  private var bookmarks: List[PDOutlineItem] = List()
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
    this(currentPage, document)
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
    this(createPage, document)
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
    val fontFile = new File(fontPath)
    PDType0Font.load(document, fontFile)
  }

  protected def getDocument: PDDocument = {
    document
  }

  def drawTitle(title: String, font: PDFont, fontSize: Int) {
    drawTitle(title, font, fontSize, null)
  }

  def drawTitle(title: String, font: PDFont, fontSize: Int, textType: Option[TextType]) {
    val articleTitle = createPdPageContentStream
    articleTitle.beginText()
    articleTitle.setFont(font, fontSize)
    articleTitle.newLineAtOffset(getMargin, yStart)
    articleTitle.setNonStrokingColor(Color.black)
    articleTitle.showText(title)
    articleTitle.endText()

    textType.getOrElse(Unit) match {
      case TextType.Highlight => throw new NotImplementedException
      case TextType.Squiggly => throw new NotImplementedException
      case TextType.Strikeout => throw new NotImplementedException
      case TextType.Underline =>
        val y: Float = (yStart - 1.5).toFloat
        val titleWidth: Float = font.getStringWidth(title) / 1000 * fontSize
        articleTitle.moveTo(getMargin, y)
        articleTitle.lineTo(getMargin + titleWidth, y)
        articleTitle.stroke()
    }

    articleTitle.close()
    yStart = (yStart - (fontSize / 1.5)).toFloat
  }

  def getWidth: Float = {
    width
  }

  def createRow(height: Float): Row = {
    val row: Row = new Row(this, height)
    this.rows = row :: this.rows
    row
  }

  def createRow(cells: List[Cell], height: Float): Row = {
    val row: Row = new Row(this, height, cells)
    this.rows = row :: this.rows
    row
  }

  def draw: Float = {
    rows.foreach(drawRow)
    endTable
    yStart
  }

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
        //logger.warn("No Header Row Defined.")
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
    row.getCells.foreach(cell => {
      if (cell.getFont == null) {
        throw new IllegalArgumentException("Font is null on Cell=" + cell.getText)
      }
      this.tableContentStream.setFont(cell.getFont, cell.getFontSize)
      this.tableContentStream.setNonStrokingColor(cell.getTextColor)
      this.tableContentStream.beginText
      this.tableContentStream.moveTextPositionByAmount(nextX, nextY)
      this.tableContentStream.appendRawCommands(cell.getParagraph.getFontHeight + " TL\n")
      val lines: List[String] = cell.getParagraph.getLines
      var numLines: Int = cell.getParagraph.getLines.size
      cell.getParagraph.getLines.foreach(line => {
        this.tableContentStream.drawString(line.trim)
        if (numLines > 0) this.tableContentStream.appendRawCommands("T*\n")
        numLines -= 1
      })
      this.tableContentStream.endText
      this.tableContentStream.closeSubPath
      nextX += cell.getWidth
    })

    yStart = yStart - row.getHeight
  }

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

  private def drawLine(`type`: String, xStart: Float, yStart: Float, xEnd: Float, yEnd: Float) {
    this.tableContentStream.setNonStrokingColor(Color.BLACK)
    this.tableContentStream.setStrokingColor(Color.BLACK)
    this.tableContentStream.drawLine(xStart, yStart, xEnd, yEnd)
    this.tableContentStream.closeSubPath
  }

  private def fillCellColor(cell: Cell, yStart: Float, xStart: Float, cellIterator: Iterator[Cell]) {
    if (cell.getFillColor != null) {
      this.tableContentStream.setNonStrokingColor(cell.getFillColor)
      val yStartWithoutCellHeight = yStart - cell.getHeight
      val height: Float = cell.getHeight - 1f
      val width: Float = getWidth(cell, cellIterator)
      this.tableContentStream.fillRect(xStart, yStartWithoutCellHeight, width + xSpacing, height)
      this.tableContentStream.closeSubPath
      this.tableContentStream.setNonStrokingColor(Color.BLACK)
    }
  }

  private def getWidth(cell: Cell, cellIterator: Iterator[Cell]): Float = {
    if (cellIterator.hasNext) {
      cell.getWidth
    } else {
      cell.getExtraWidth
    }
  }

  protected def createPage: T

  private def endTable {
    if (drawLines) {
      drawLine("Row Bottom Border ", this.margin, this.yStart, this.margin + width + xSpacing, this.yStart)
    }

    this.tableContentStream.close()
  }

  def getCurrentPage: T = {
    if (this.currentPage == null) throw new Exception("No current page defined.")

    this.currentPage
  }

  def isEndOfPage(row: Row): Boolean = {
    val currentY: Float = yStart - row.getHeight
    val isEndOfPage: Boolean = currentY <= (bottomMargin + 10)
    isEndOfPage
  }

  private def addBookmark(bookmark: PDOutlineItem) {
    bookmarks = bookmark::bookmarks
  }

  def getBookmarks: List[PDOutlineItem] = {
    bookmarks
  }

  def setHeader(header: Row) {
    this.header = header
  }

  def getHeader: Row = {
    if (header == null) {
      throw new IllegalArgumentException("Header Row not set on table")
    }

    header
  }

  private[boxable] def getMargin: Float = {
    margin
  }

  protected def setYStart(yStart: Float) {
    this.yStart = yStart
  }
}
