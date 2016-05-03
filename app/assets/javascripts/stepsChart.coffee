# Load the Visualization API and the piechart package.
# Callback that creates and populates a data table,
# instantiates the pie chart, passes in the data and
# draws it.

renderChart = (data, options) ->
  window.chart.draw data, options

window.stepSelected = 1

window.initChart = ->

# Create the data table.
  data = new (google.visualization.DataTable)

  data.addColumn('number', "step")
  data.addColumn('number', "count")

  for stepObj in window.stepSummaries
    data.addRow([stepObj.step, stepObj.stepStarted - stepObj.stepRestarted - stepObj.stepCompleted])

  selectHandler = ->
    selectedItem = chart.getSelection()[0]
    window.stepSelected = selectedItem.row + 1
    showStepDetail()
    return

  w = $("#chart_div").parent().width()
  options =
    backgroundColor: "#252830"
    legend: 'none'
    width: w
    height: 340
    hAxis:
      title: ""
      gridlines:
        color: '#6f7890'
    vAxis:
      title: ""
      gridlines:
        color: '#6f7890'
    bar:
      groupWidth: '75%'
    isStacked: true
    animation:
      startup: true
      duration: 1000
      easing: 'out'

  # Instantiate and draw our chart, passing in some options.
  window.chart = new (google.charts.Bar)(document.getElementById('chart_div'))

  google.visualization.events.addListener chart, 'select', selectHandler

  initializeProgressChart(window.progressData)
  renderChart(data, google.charts.Bar.convertOptions(options))

  feed = new EventSource('/feed')
  feed.addEventListener 'message', ((event) ->
    result = jQuery.parseJSON(event.data)
    data = new (google.visualization.DataTable)
    data.addColumn('number', "step")
    data.addColumn('number', "count")
    showQuickStats(result)
    showProgress(result)
    for stepObj in result.stepSummaries
      data.addRow([stepObj._id, stepObj.stepStarted - stepObj.stepCompleted - stepObj.stepRestarted])

    renderChart(data, google.charts.Bar.convertOptions(options))
    return
  ), false

  return

showStepDetail = =>
  summ = window.stepSummaries
  console.log summ
  $("#stepDetailNumber").text("step #{window.stepSelected}")


initializeProgressChart = (data) =>
  ctx = document.getElementById("progressChart").getContext("2d")
  $("#progressPercent").text((data[1].value / 100 * data[0].value).toFixed(2) + "%")
  window.progressChart = new Chart(ctx).Doughnut(data, {
    percentageInnerCutout: 70,
    segmentShowStroke: false,
    animationSteps: 30,
  });

showProgress = (data) =>
  window.progressChart.segments[0].value = data.quickStats.finished
  window.progressChart.segments[1].value = data.quickStats.started
  window.progressChart.update()
  $("#progressPercent").text((data.quickStats.started / 100 * data.quickStats.finished).toFixed(2) + "%")


showQuickStats = (data) =>
  $('#QuickStatsStarted').text(data.quickStats.started)
  $('#QuickStatsStalled').text(data.quickStats.stalled)
  zero = "0.0%"
  if data.quickStats.stalled > 0
    $('#QuickStatsStalledPerc').text((data.quickStats.started / 100 * data.quickStats.stalled).toFixed(2) + '%')
  else
    $('#QuickStatsStalledPerc').text(zero)

  $('#QuickStatsRestarted').text(data.quickStats.restarted)
  if data.quickStats.restarted > 0
    $('#QuickStatsRestartedPerc').text((data.quickStats.started / 100 * data.quickStats.restarted).toFixed(2) + '%')
  else
    $('#QuickStatsRestartedPerc').text(zero)

  $('#QuickStatsFinished').text(data.quickStats.finished)
  if data.quickStats.finished > 0
    $('#QuickStatsFinishedPerc').text((data.quickStats.started / 100 * data.quickStats.finished).toFixed(2) + '%')
  else
    $('#QuickStatsFinishedPerc').text(zero)

  console.log($('#QuickStatsStarted').val())

