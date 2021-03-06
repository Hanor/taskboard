/**
 * Taskboard version of dc.pieChart
 */

/**
 * The pie chart implementation is usually used to visualize a small categorical distribution.  The pie
 * chart uses keyAccessor to determine the slices, and valueAccessor to calculate the size of each
 * slice relative to the sum of all values. Slices are ordered by {@link dc.baseMixin#ordering ordering}
 * which defaults to sorting by key.
 *
 * Examples:
 * - {@link http://dc-js.github.com/dc.js/ Nasdaq 100 Index}
 * @class pieChart
 * @memberof dc
 * @mixes dc.capMixin
 * @mixes dc.colorMixin
 * @mixes dc.baseMixin
 * @example
 * // create a pie chart under #chart-container1 element using the default global chart group
 * var chart1 = dc.pieChart('#chart-container1');
 * // create a pie chart under #chart-container2 element using chart group A
 * var chart2 = dc.pieChart('#chart-container2', 'chartGroupA');
 * @param {String|node|d3.selection} parent - Any valid
 * {@link https://github.com/d3/d3-3.x-api-reference/blob/master/Selections.md#selecting-elements d3 single selector} specifying
 * a dom block element such as a div; or a dom element or d3 selection.
 * @param {String} [chartGroup] - The name of the chart group this chart instance should be placed in.
 * Interaction with a chart will only trigger events and redraws within the chart's group.
 * @returns {dc.pieChart}
 */
dc.tbPieChart = function (parent, chartGroup) {
    var _tbLabelCssClass = 'tb-label', // "_labelCssClass = 'pie-label'" removed
        _tbSelectedClass = 'selected',
        _tbDeselectedClass = 'deselected';

    var DEFAULT_MIN_ANGLE_FOR_LABEL = 0.5;

    var _sliceCssClass = 'pie-slice';
    var _sliceGroupCssClass = 'pie-slice-group';
    var _labelGroupCssClass = 'pie-label-group';
    var _emptyCssClass = 'empty-chart';
    var _emptyTitle = 'empty';

    var _radius,
        _givenRadius, // specified radius, if any
        _innerRadius = 0,
        _externalRadiusPadding = 0;

    var _g;
    var _cx;
    var _cy;
    var _minAngleForLabel = DEFAULT_MIN_ANGLE_FOR_LABEL;
    var _externalLabelRadius;
    var _drawPaths = false;
    var _chart = dc.capMixin(dc.colorMixin(dc.baseMixin({})));

    _chart.colorAccessor(_chart.cappedKeyAccessor);

    _chart.title(function (d) {
        return _chart.cappedKeyAccessor(d) + ': ' + _chart.cappedValueAccessor(d);
    });

    /**
     * Get or set the maximum number of slices the pie chart will generate. The top slices are determined by
     * value from high to low. Other slices exeeding the cap will be rolled up into one single *Others* slice.
     * @method slicesCap
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [cap]
     * @returns {Number|dc.pieChart}
     */
    _chart.slicesCap = _chart.cap;

    _chart.label(_chart.cappedKeyAccessor);
    _chart.renderLabel(true);

    _chart.transitionDuration(350);
    _chart.transitionDelay(0);

    _chart._doRender = function () {
        _chart.resetSvg();

        _g = _chart.svg()
            .append('g')
            .attr('transform', 'translate(' + _chart.cx() + ',' + _chart.cy() + ')');

        _g.append('g').attr('class', _sliceGroupCssClass);
        _g.append('g').attr('class', _labelGroupCssClass);

        drawChart();

        return _chart;
    };

    function drawChart () {
        // set radius from chart size if none given, or if given radius is too large
        var maxRadius =  d3.min([_chart.width(), _chart.height()]) / 2;
        _radius = _givenRadius && _givenRadius < maxRadius ? _givenRadius : maxRadius;

        var arc = buildArcs();

        var pie = pieLayout();
        var pieData;
        // if we have data...
        if (d3.sum(_chart.data(), _chart.valueAccessor())) {
            pieData = pie(_chart.data());
            _g.classed(_emptyCssClass, false);
        } else {
            // otherwise we'd be getting NaNs, so override
            // note: abuse others for its ignoring the value accessor
            pieData = pie([{key: _emptyTitle, value: 1, others: [_emptyTitle]}]);
            _g.classed(_emptyCssClass, true);
        }

        if (_g) {
            var slices = _g.select('g.' + _sliceGroupCssClass)
                .selectAll('g.' + _sliceCssClass)
                .data(pieData);

            var labels = _g.select('g.' + _labelGroupCssClass)
                .selectAll('g.' + _tbLabelCssClass)
                .data(pieData);

            createElements(slices, labels, arc, pieData);

            updateElements(pieData, arc);

            removeElements(slices, labels);

            highlightFilter();

            highlightLabels();

            dc.transition(_g, _chart.transitionDuration(), _chart.transitionDelay())
                .attr('transform', 'translate(' + _chart.cx() + ',' + _chart.cy() + ')');
        }
    }

    function createElements (slices, labels, arc, pieData) {
        var slicesEnter = createSliceNodes(slices);

        createSlicePath(slicesEnter, arc);

        createTitles(slicesEnter);

        createLabels(labels, pieData, arc);
    }

    function createSliceNodes (slices) {
        var slicesEnter = slices
            .enter()
            .append('g')
            .attr('class', function (d, i) {
                return _sliceCssClass + ' _' + i;
            });
        return slicesEnter;
    }

    function createSlicePath (slicesEnter, arc) {
        var slicePath = slicesEnter.append('path')
            .attr('fill', fill)
            .on('click', onClick)
            .attr('d', function (d, i) {
                return safeArc(d, i, arc);
            })
            .on('mouseover', function (d, i) {
                highlightSlice(i, true);
                highlightLabel(i, true);
            })
            .on('mouseout', function (d, i) {
                highlightSlice(i, false);
                highlightLabel(i, false);
            });

        var transition = dc.transition(slicePath, _chart.transitionDuration(), _chart.transitionDelay());
        if (transition.attrTween) {
            transition.attrTween('d', tweenPie);
        }
    }

    function createTitles (slicesEnter) {
        if (_chart.renderTitle()) {
            slicesEnter.append('title').text(function (d) {
                return _chart.title()(d.data);
            });
        }
    }

    _chart._applyLabelText = function (labels) {
        labels
            .text(function (d) {
                var data = d.data;
                if ((sliceHasNoData(data)) && !isSelectedSlice(d)) {
                    return '';
                }
                return _chart.label()(d.data);
            });
    };

    function positionLabels (labels, arc) {
        dc.transition(labels, _chart.transitionDuration(), _chart.transitionDelay())
            .attr('transform', function (d) {
                return labelPosition(d, arc);
            });
    }

    function highlightSlice (index, whether) {
        _chart.selectAll('g.pie-slice').each(function (d, i) {
            if (index === i && whether) {
                this.classList.add('highlight');
                this.classList.remove('non-highlight');
            } else if (index !== i && whether) {
                this.classList.add('non-highlight');
                this.classList.remove('highlight');
            } else {
                this.classList.remove('highlight');
                this.classList.remove('non-highlight');
            }
        });
    }

    function createLabels (labels, pieData, arc) {
        if (_chart.renderLabel()) {
            var labelsEnter = labels.enter();

            var labelsGroups = labelsEnter.append('g')
                .attr('class', _tbLabelCssClass)
                .each(function (d) {
                    if (sliceTooSmall(d))
                        this.classList.add('no-label');
                });

            var labelRects = labelsGroups.append('rect');

            var labelsText = labelsGroups.append('text')
                .attr('text-anchor', 'middle')
                .attr('class', function (d, i) {
                    var classes = _sliceCssClass + ' ' + _tbLabelCssClass + ' _' + i;
                    if (_externalLabelRadius) {
                        classes += ' external';
                    }
                    return classes;
                })
                .on('click', onClick)
                .on('mouseover', function (d, i) {
                    highlightSlice(i, true);
                    highlightLabel(i, true);
                })
                .on('mouseout', function (d, i) {
                    highlightSlice(i, false);
                    highlightLabel(i, false);
                });

            _chart._applyLabelText(labelsText);
            positionLabels(labelsGroups, arc);
            if (_externalLabelRadius && _drawPaths) {
                updateLabelPaths(pieData, arc);
            }

            labelsText.each(function(d) {
                d.bb = this.getBBox(); // get bounding box of text field and store it in texts array
            });

            var paddingLeftRight = 12; // adjust the padding values depending on font and font size
            var paddingTopBottom = 6;

            labelRects
                .attr("x", function(d) { return !sliceHasNoData(d) ? d.bb.x - paddingLeftRight/2 : 0; })
                .attr("y", function(d) { return !sliceHasNoData(d) ? d.bb.y - paddingTopBottom/2 : 0; })
                .attr("width", function(d) { return !sliceHasNoData(d) ? d.bb.width + paddingLeftRight : 0; })
                .attr("height", function(d) { return !sliceHasNoData(d) ? d.bb.height + paddingTopBottom : 0; });
        }
    }

    function updateLabelPaths (pieData, arc) {
        var polyline = _g.selectAll('polyline.' + _sliceCssClass)
            .data(pieData);

        polyline
            .enter()
            .append('polyline')
            .attr('class', function (d, i) {
                return 'pie-path _' + i + ' ' + _sliceCssClass;
            })
            .on('click', onClick)
            .on('mouseover', function (d, i) {
                highlightSlice(i, true);
                highlightLabel(i, true);
            })
            .on('mouseout', function (d, i) {
                highlightSlice(i, false);
                highlightLabel(i, false);
            });

        polyline.exit().remove();
        var arc2 = d3.svg.arc()
            .outerRadius(_radius - _externalRadiusPadding + _externalLabelRadius)
            .innerRadius(_radius - _externalRadiusPadding);
        var transition = dc.transition(polyline, _chart.transitionDuration(), _chart.transitionDelay());
        // this is one rare case where d3.selection differs from d3.transition
        if (transition.attrTween) {
            transition
                .attrTween('points', function (d) {
                    var current = this._current || d;
                    current = {startAngle: current.startAngle, endAngle: current.endAngle};
                    var interpolate = d3.interpolate(current, d);
                    this._current = interpolate(0);
                    return function (t) {
                        var d2 = interpolate(t);
                        return [arc.centroid(d2), arc2.centroid(d2)];
                    };
                });
        } else {
            transition.attr('points', function (d) {
                return [arc.centroid(d), arc2.centroid(d)];
            });
        }
        transition.style('visibility', function (d) {
            return d.endAngle - d.startAngle < 0.0001 ? 'hidden' : 'visible';
        });

    }

    function updateElements (pieData, arc) {
        updateSlicePaths(pieData, arc);
        updateLabels(pieData, arc);
        updateTitles(pieData);
    }

    function updateSlicePaths (pieData, arc) {
        var slicePaths = _g.selectAll('g.' + _sliceCssClass)
            .data(pieData)
            .select('path')
            .attr('d', function (d, i) {
                return safeArc(d, i, arc);
            });
        var transition = dc.transition(slicePaths, _chart.transitionDuration(), _chart.transitionDelay());
        if (transition.attrTween) {
            transition.attrTween('d', tweenPie);
        }
        transition.attr('fill', fill);
    }

    function updateLabels (pieData, arc) {
        if (_chart.renderLabel()) {
            var labels = _g.selectAll('g.' + _tbLabelCssClass)
                .data(pieData);
            positionLabels(labels, arc);
            if (_externalLabelRadius && _drawPaths) {
                updateLabelPaths(pieData, arc);
            }
        }
    }

    function updateTitles (pieData) {
        if (_chart.renderTitle()) {
            _g.selectAll('g.' + _sliceCssClass)
                .data(pieData)
                .select('title')
                .text(function (d) {
                    return _chart.title()(d.data);
                });
        }
    }

    function removeElements (slices, labels) {
        slices.exit().remove();
        labels.exit().remove();
    }

    function highlightFilter () {
        if (_chart.hasFilter()) {
            _chart.selectAll('g.' + _sliceCssClass).each(function (d) {
                if (isSelectedSlice(d)) {
                    _chart.highlightSelected(this);
                } else {
                    _chart.fadeDeselected(this);
                }
            });
        } else {
            _chart.selectAll('g.' + _sliceCssClass).each(function () {
                _chart.resetHighlight(this);
            });
        }
    }

    function highlightLabels () {
        if (_chart.hasFilter()) {
            _chart.selectAll('g.' + _tbLabelCssClass).each(function (d) {
                if (isSelectedSlice(d)) {
                    selectLabel(this);
                } else {
                    deselectLabel(this);
                }
            });
        } else {
            _chart.selectAll('g.' + _tbLabelCssClass).each(function () {
                resetSelectionLabel(this);
            });
        }
    }

    function selectLabel (label) {
        label.classList.add(_tbSelectedClass);
        label.classList.remove(_tbDeselectedClass);
    }

    function deselectLabel (label) {
        label.classList.add(_tbDeselectedClass);
        label.classList.remove(_tbSelectedClass);
    }

    function resetSelectionLabel (label) {
        label.classList.remove(_tbSelectedClass);
        label.classList.remove(_tbDeselectedClass);
    }

    function highlightLabel (index, whether) {
        _chart.selectAll('g.' + _tbLabelCssClass).each(function (d, i) {
            if (index === i && whether) {
                this.classList.add('highlight');
                this.classList.remove('non-highlight');
            } else if (index !== i && whether) {
                this.classList.add('non-highlight');
                this.classList.remove('highlight');
            } else {
                this.classList.remove('highlight');
                this.classList.remove('non-highlight');
            }
        });
    }


    /**
     * Get or set the external radius padding of the pie chart. This will force the radius of the
     * pie chart to become smaller or larger depending on the value.
     * @method externalRadiusPadding
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [externalRadiusPadding=0]
     * @returns {Number|dc.pieChart}
     */
    _chart.externalRadiusPadding = function (externalRadiusPadding) {
        if (!arguments.length) {
            return _externalRadiusPadding;
        }
        _externalRadiusPadding = externalRadiusPadding;
        return _chart;
    };

    /**
     * Get or set the inner radius of the pie chart. If the inner radius is greater than 0px then the
     * pie chart will be rendered as a doughnut chart.
     * @method innerRadius
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [innerRadius=0]
     * @returns {Number|dc.pieChart}
     */
    _chart.innerRadius = function (innerRadius) {
        if (!arguments.length) {
            return _innerRadius;
        }
        _innerRadius = innerRadius;
        return _chart;
    };

    /**
     * Get or set the outer radius. If the radius is not set, it will be half of the minimum of the
     * chart width and height.
     * @method radius
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [radius]
     * @returns {Number|dc.pieChart}
     */
    _chart.radius = function (radius) {
        if (!arguments.length) {
            return _givenRadius;
        }
        _givenRadius = radius;
        return _chart;
    };

    /**
     * Get or set center x coordinate position. Default is center of svg.
     * @method cx
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [cx]
     * @returns {Number|dc.pieChart}
     */
    _chart.cx = function (cx) {
        if (!arguments.length) {
            return (_cx ||  _chart.width() / 2);
        }
        _cx = cx;
        return _chart;
    };

    /**
     * Get or set center y coordinate position. Default is center of svg.
     * @method cy
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [cy]
     * @returns {Number|dc.pieChart}
     */
    _chart.cy = function (cy) {
        if (!arguments.length) {
            return (_cy ||  _chart.height() / 2);
        }
        _cy = cy;
        return _chart;
    };

    function buildArcs () {
        return d3.svg.arc()
            .outerRadius(_radius - _externalRadiusPadding)
            .innerRadius(_innerRadius);
    }

    function isSelectedSlice (d) {
        return _chart.hasFilter(_chart.cappedKeyAccessor(d.data));
    }

    _chart._doRedraw = function () {
        drawChart();
        return _chart;
    };

    /**
     * Get or set the minimal slice angle for label rendering. Any slice with a smaller angle will not
     * display a slice label.
     * @method minAngleForLabel
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [minAngleForLabel=0.5]
     * @returns {Number|dc.pieChart}
     */
    _chart.minAngleForLabel = function (minAngleForLabel) {
        if (!arguments.length) {
            return _minAngleForLabel;
        }
        _minAngleForLabel = minAngleForLabel;
        return _chart;
    };

    function pieLayout () {
        return d3.layout.pie().sort(null).value(_chart.cappedValueAccessor);
    }

    function sliceTooSmall (d) {
        var angle = (d.endAngle - d.startAngle);
        return isNaN(angle) || angle < _minAngleForLabel;
    }

    function sliceHasNoData (d) {
        return _chart.cappedValueAccessor(d) === 0;
    }

    function tweenPie (b) {
        b.innerRadius = _innerRadius;
        var current = this._current;
        if (isOffCanvas(current)) {
            current = {startAngle: 0, endAngle: 0};
        } else {
            // only interpolate startAngle & endAngle, not the whole data object
            current = {startAngle: current.startAngle, endAngle: current.endAngle};
        }
        var i = d3.interpolate(current, b);
        this._current = i(0);
        return function (t) {
            return safeArc(i(t), 0, buildArcs());
        };
    }

    function isOffCanvas (current) {
        return !current || isNaN(current.startAngle) || isNaN(current.endAngle);
    }

    function fill (d, i) {
        return _chart.getColor(d.data, i);
    }

    function onClick (d, i) {
        if (_g.attr('class') !== _emptyCssClass) {
            _chart.onClick(d.data, i);
        }
    }

    function safeArc (d, i, arc) {
        var path = arc(d, i);
        if (path.indexOf('NaN') >= 0) {
            path = 'M0,0';
        }
        return path;
    }

    /**
     * Title to use for the only slice when there is no data.
     * @method emptyTitle
     * @memberof dc.pieChart
     * @instance
     * @param {String} [title]
     * @returns {String|dc.pieChart}
     */
    _chart.emptyTitle = function (title) {
        if (arguments.length === 0) {
            return _emptyTitle;
        }
        _emptyTitle = title;
        return _chart;
    };

    /**
     * Position slice labels offset from the outer edge of the chart.
     *
     * The argument specifies the extra radius to be added for slice labels.
     * @method externalLabels
     * @memberof dc.pieChart
     * @instance
     * @param {Number} [externalLabelRadius]
     * @returns {Number|dc.pieChart}
     */
    _chart.externalLabels = function (externalLabelRadius) {
        if (arguments.length === 0) {
            return _externalLabelRadius;
        } else if (externalLabelRadius) {
            _externalLabelRadius = externalLabelRadius;
        } else {
            _externalLabelRadius = undefined;
        }

        return _chart;
    };

    /**
     * Get or set whether to draw lines from pie slices to their labels.
     *
     * @method drawPaths
     * @memberof dc.pieChart
     * @instance
     * @param {Boolean} [drawPaths]
     * @returns {Boolean|dc.pieChart}
     */
    _chart.drawPaths = function (drawPaths) {
        if (arguments.length === 0) {
            return _drawPaths;
        }
        _drawPaths = drawPaths;
        return _chart;
    };

    function labelPosition (d, arc) {
        var centroid;
        if (_externalLabelRadius) {
            centroid = d3.svg.arc()
                .outerRadius(_radius - _externalRadiusPadding + _externalLabelRadius)
                .innerRadius(_radius - _externalRadiusPadding + _externalLabelRadius)
                .centroid(d);
        } else {
            centroid = arc.centroid(d);
        }
        if (isNaN(centroid[0]) || isNaN(centroid[1])) {
            return 'translate(0,0)';
        } else {
            return 'translate(' + centroid + ')';
        }
    }

    _chart.legendables = function () {
        return _chart.data().map(function (d, i) {
            var legendable = {name: d.key, data: d.value, others: d.others, chart: _chart};
            legendable.color = _chart.getColor(d, i);
            return legendable;
        });
    };

    _chart.legendHighlight = function (d) {
        highlightSliceFromLegendable(d, true);
    };

    _chart.legendReset = function (d) {
        highlightSliceFromLegendable(d, false);
    };

    _chart.legendToggle = function (d) {
        _chart.onClick({key: d.name, others: d.others});
    };

    function highlightSliceFromLegendable (legendable, highlighted) {
        _chart.selectAll('g.pie-slice').each(function (d, i) {
            if (legendable.name === d.data.key) {
                highlightSlice(i, highlighted)
                highlightLabel(i, highlighted);
            }
        });
    }

    return _chart.anchor(parent, chartGroup);
};