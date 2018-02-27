function clone(obj) {
    return jQuery.extend(true, {}, obj);
}

function sortByProperty(property) {
    return function(a,b) { return a[property] - b[property] };
}

function forEachInArray(array, callback) {
    var arrayLength = array.length;
    for (var i = 0; i < arrayLength; i++)
        callback(array[i])
}

function findInArray(array, callback) {
    var notFoundValue = undefined;
    var arrayLength = array.length;
    for (var i = 0; i < arrayLength; i++)
        if(callback(array[i]))
            return array[i];
    return notFoundValue;
}

function findIndexInArray(array, callback) {
    var notFoundValue = -1;
    var arrayLength = array.length;
    for (var i = 0; i < arrayLength; i++)
        if(callback(array[i]))
            return i;
    return notFoundValue;
}

function copyProperties(receiverObject, objectToMarge) {
    $.extend(receiverObject, objectToMarge);
}

function isFieldNameEquals(fieldName, anotherFieldName) {
    return fieldName.toLowerCase === anotherFieldName.toLowerCase;
}

function getDateFromYYYYMMDD(yyyymmdd) {
    if (!yyyymmdd || yyyymmdd.length !== 8)
        return yyyymmdd + " (invalid date format)";
    var year = yyyymmdd.substr(0, 4);
    var month = yyyymmdd.substr(4, 2) - 1;
    var day = yyyymmdd.substr(6, 2);
    return new Date(year, month, day);
}

function addDaysToDate(date, days) {
    var newDate = new Date(date.valueOf());
    newDate.setDate(newDate.getDate() + days);
    return newDate;
}

if (!Object.values) {
    Object.values = function(obj) {
        return Object.keys(obj).map(
            function(key) { return obj[key] }
        );
    };
}

if (!Array.prototype.findIndex) {
    // https://developer.mozilla.org/pt-BR/docs/Web/JavaScript/Reference/Global_Objects/Array/findIndex#Polyfill
    Array.prototype.findIndex = function(predicate) {
        if (this === null) {
            throw new TypeError('Array.prototype.findIndex called on null or undefined');
        }
        if (typeof predicate !== 'function') {
            throw new TypeError('predicate must be a function');
        }
        var list = Object(this);
        var length = list.length >>> 0;
        var thisArg = arguments[1];
        var value;

        for (var i = 0; i < length; i++) {
            value = list[i];
            if (predicate.call(thisArg, value, i, list)) {
                return i;
            }
        }
        return -1;
    };
}

function rangesEqual(range1, range2) {
    if (!range1 && !range2) {
        return true;
    }
    else if (!range1 || !range2) {
        return false;
    }
    else if (range1.length === 0 && range2.length === 0) {
        return true;
    }
    else if (range1[0].valueOf() === range2[0].valueOf() &&
        range1[1].valueOf() === range2[1].valueOf()) {
        return true;
    }
    return false;
}

function setupChartDateTicks(chart, startDate, endDate, tickCount) {
    chart.xAxis()
        .tickValues(
            dcUtils.getDateTicks(startDate, endDate, tickCount)
        )
        .tickFormat(function (v) {
            return dcUtils.dateFormat(v);
        });
    chart.visibleTickCount = tickCount;
}