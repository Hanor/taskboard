function DcDateRangeChartsService() {

    var self = this;

    self._BRUSH_DAYS_LIMIT = 365 * 2;

    self._rangeChart = null;

    self._registeredCharts = new Map();

    self._isBrushing = false;

    self.resizeOptions = {
        LEFT: 'left',
        RIGHT: 'right'
    };

    self._lastResizeSideHovered = self.resizeOptions.LEFT;

    self._brushRange = {
        start: null,
        end: null,
        value: function() {
            return [this.start, this.end];
        },
        copyRange: function(range) {
            this.start = range[0];
            this.end = range[1];
        },
        isValidRange: function() {
            return dateDiffInDays(this.start, this.end) <= self._BRUSH_DAYS_LIMIT
        }
    };

    self.setDateRangeChart = function(rangeChart) {
        self._isBrushing = false;
        self._rangeChart = rangeChart;
        self._rangeChart.brushOn(true);
        self._rangeChart.brush().on('brushstart.resize', self._onBrushStart.bind(self));
        self._rangeChart.brush().on('brushend.resize', self._onBrushEnd.bind(self));
        self._rangeChart.on('filtered', self._onFiltered.bind(self));
        self._registerBrushResizeHoverHandler();
        self._resetSelection();
    };

    self.registerChartInRangeAndRender = function(chartToRegister) {
        chartToRegister.render();
        if (self._rangeChart)
            self._applyFocusOnChart(chartToRegister);

        self._registeredCharts.set(chartToRegister.anchorName(), chartToRegister);
    };

    self.deregisterChartInRange = function(chartToDeregister) {
        self._registeredCharts.delete(chartToDeregister.anchorName());
    };

    self._onBrushStart = function() {
        self._isBrushing = true;
    };

    self._onBrushEnd = function() {
        self._isBrushing = false;
        self._brushRange.copyRange(self._rangeChart.extendBrush());
        if (!self._brushRange.isValidRange()) {
            if (self._lastResizeSideHovered === self.resizeOptions.RIGHT)
                self._brushRange.start = removeDaysFromDate(self._brushRange.end, self._BRUSH_DAYS_LIMIT);
            else
                self._brushRange.end = addDaysToDate(self._brushRange.start, self._BRUSH_DAYS_LIMIT);
            self._rangeChart.filter(self._brushRange.value());
        }
        self._onFiltered();
    };

    self._applyFocusOnChart = function(chartToFocus) {
        chartToFocus.focus(self._brushRange.value());
        self._updateTicksAndRender(chartToFocus);
    };

    self._updateTicksAndRender = function(chart) {
        chart
            .xAxis()
            .tickValues(dcUtils.getDateTicks(chart.xAxisMin(), chart.xAxisMax(), chart.visibleTickCount));
        chart.render();
    };

    self._onFiltered = function() {
        if (self._isBrushing)
            return;

        if (!self._rangeChart.filter() || !self._rangeChart.brushOn())
            self._resetSelection();
        else
            self._applySelection();
    };

    self._applySelection = function() {
        self._registeredCharts.forEach(function(registeredChart) {
            if (!dcUtils.rangesEqual(self._brushRange.value(), registeredChart.filter())) {
                dc.events.trigger(function () {
                    self._applyFocusOnChart(registeredChart);
                });
            }
        });
    };

    self._resetSelection = function() {
        dc.events.trigger(function () {
            self._brushRange.copyRange(self._rangeChart.xOriginalDomain());
            if (!self._brushRange.isValidRange())
                self._brushRange.start = removeDaysFromDate(self._brushRange.end, self._BRUSH_DAYS_LIMIT);

            self._rangeChart.filter(self._brushRange.value());
        });
    };

    self._registerBrushResizeHoverHandler = function () {
        self._rangeChart.on('renderlet.resize', function() {
            self._rangeChart.select('g.resize.w').on('mouseover.resize', function () {
                self._lastResizeSideHovered = self.resizeOptions.LEFT;
            });
            self._rangeChart.select('g.resize.e').on('mouseover.resize', function () {
                self._lastResizeSideHovered = self.resizeOptions.RIGHT;
            });
        });
    };

}

var dcDateRangeChartsService = new DcDateRangeChartsService();
