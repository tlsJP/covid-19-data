package com.jp.covid.data

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.collections.transformation.SortedList
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.text.Font
import javafx.stage.Stage
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.FileReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.StreamSupport
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Build using: ./gradlew build
 * Run using: ./gradlew run --args="new york"
 * OR
 * ./gradlew run --args "new york, washington, virginia"
 */
class NewCasesPerDay : Application() {

    companion object {
        private var states: Collection<String>? = null

        private val map: MutableMap<String, MutableCollection<StateRecord>> = HashMap()

        @JvmStatic
        fun main(args: Array<String>) {
            val input = args.joinToString(" ")
            states = input.split(",").map { s -> s.toLowerCase().trim() }
            (states as List<String>).stream().forEach { s -> println(s) }

            launch(NewCasesPerDay::class.java)
        }
    }

    override fun start(stage: Stage) {

        stage.title = "Covid-19 Chart"

        // Create chart
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        xAxis.label = "Date"
        xAxis.tickLabelFont = Font.font(15.0)
        yAxis.label = "Cases"
        val lineChart = LineChart(xAxis, yAxis)
//        lineChart.axisSortingPolicy = LineChart.SortingPolicy.NONE

        // Get Data
        val fr = FileReader("us-states.csv")
        val records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fr)
        val spliterator = Spliterators.spliteratorUnknownSize(records.iterator(), Spliterator.ORDERED)
        val csvStream = StreamSupport.stream(spliterator, false)

        csvStream.filter { r -> states!!.contains(r.get("state").toLowerCase()) }
                .map { r -> getStateFromCsv(r) }
                .forEach { r -> addToMap(r) }

        map.entries.map { e -> getSeriesFromState(e.key, e.value) }
                .flatten()
                .forEach { s -> lineChart.data.add(s) }

        val scene = Scene(lineChart, 800.0, 600.0)

        stage.scene = scene
        stage.show()
    }

    private fun addToMap(r: StateRecord) {

        if (!map.containsKey(r.state)) {
            map[r.state!!] = ArrayList()
        }

        map[r.state]!!.add(r)
    }

    private fun getStateFromCsv(r: CSVRecord): StateRecord {
        val result = StateRecord()
        result.date = LocalDate.parse(r.get("date"), DateTimeFormatter.ISO_DATE)
        result.totalCases = Integer.parseInt(r.get("cases"))
        result.state = r.get("state")
        result.deaths = Integer.parseInt(r.get("deaths"))

        return result
    }

    private fun getSeriesFromState(state: String, records: MutableCollection<StateRecord>): MutableCollection<XYChart.Series<String, Number>> {

        val result: MutableCollection<XYChart.Series<String, Number>> = ArrayList()

        val pdData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val tData = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
        val dData = FXCollections.observableArrayList<XYChart.Data<String,Number>>()

        var floor = 0
        for (rec in records) {
            val perDay = rec.totalCases!! - floor
            floor = rec.totalCases!!

            if (rec.date!!.isBefore(LocalDate.of(2020, 3, 7))) {
                continue
            }

            pdData.add(XYChart.Data(rec.date.toString(), perDay))
            tData.add(XYChart.Data(rec.date.toString(), rec.totalCases!!))
            dData.add(XYChart.Data(rec.date.toString(),rec.deaths!!))
        }

        val comparator = kotlin.Comparator<XYChart.Data<String,Number>>() { o1, o2 -> o1.xValue.compareTo(o2.xValue) }

        val sortedTData = SortedList(tData, comparator)
        val sortedPdData = SortedList(pdData, comparator)
        val sortedDData = SortedList(dData, comparator)

        val perDaySeries = XYChart.Series<String, Number>()
        perDaySeries.name = "$state New Cases"
        perDaySeries.data = sortedPdData

        val totalCountSeries = XYChart.Series<String, Number>()
        totalCountSeries.name = "$state Total"
        totalCountSeries.data = sortedTData

        val deaths = XYChart.Series<String,Number>()
        deaths.name = "$state Deaths"
        deaths.data = sortedDData

        result.add(perDaySeries)
        result.add(totalCountSeries)
//        result.add(deaths)

        return result
    }
}