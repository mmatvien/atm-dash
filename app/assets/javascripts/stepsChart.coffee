# Load the Visualization API and the piechart package.
# Callback that creates and populates a data table,
# instantiates the pie chart, passes in the data and
# draws it.

renderChart = (data, options) ->
  window.chart.draw data, options


window.initChart = ->

  # Create the data table.
  data = new (google.visualization.DataTable)

  data.addColumn('number', "step")
  data.addColumn('number', "count")

  for stepObj in window.stepSummaries
    data.addRow([stepObj.step, stepObj.count])

  selectHandler = ->
    selectedItem = chart.getSelection()[0]
    if selectedItem
      topping = data.getValue(selectedItem.row, 0)
      alert 'The user selected ' + topping
      data.addRows [ [
        'bogus'
        3
      ] ]
      chart.draw data, options
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

  renderChart(data, google.charts.Bar.convertOptions(options))

  feed = new EventSource('/feed')
  feed.addEventListener 'message', ((event) ->
    result = jQuery.parseJSON(event.data)
    data = new (google.visualization.DataTable)
    data.addColumn('number', "step")
    data.addColumn('number', "count")
    console.log (result)
    for stepObj in result.stepSummaries
      data.addRow([stepObj._id, stepObj.stepStarted - stepObj.stepCompleted])

    renderChart( data, google.charts.Bar.convertOptions(options))
    return
  ), false

  return

