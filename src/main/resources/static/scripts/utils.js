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

function filterInArray(array, callback) {
    var res = [];
    var arrayLength = array.length;
    for (var i = 0; i < arrayLength; i++)
        if(callback(array[i]))
            res.push(array[i]);
    return res;
}


function isEmptyArray(array) {
    if (!Array.isArray(array))
        throw new TypeError('This value ('+ array +') is not an array');

    return array.length === 0;

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

function getDateFromIso(isoString) {
    return moment(isoString, 'YYYY-MM-DD').toDate();
}

function addDaysToDate(date, days) {
    var newDate = new Date(date.valueOf());
    newDate.setDate(newDate.getDate() + days);
    return newDate;
}

function removeDaysFromDate(date, days) {
    var newDate = new Date(date.valueOf());
    newDate.setDate(newDate.getDate() - days);
    return newDate;
}

function getCapitalized(string) {
    return _.isEmpty(string) ? string : string.charAt(0).toUpperCase() + string.slice(1);
}

function makeSVGEl(tag, attrs) {
    var el = document.createElementNS('http://www.w3.org/2000/svg', tag);
    forEachInArray(Object.keys(attrs), function (key) {
        el.setAttribute(key, attrs[key]);
    });
    return el;
}

var _MS_PER_DAY = 1000 * 60 * 60 * 24;
function dateDiffInDays(dateA, dateB) {
    var utc1 = Date.UTC(dateA.getFullYear(), dateA.getMonth(), dateA.getDate());
    var utc2 = Date.UTC(dateB.getFullYear(), dateB.getMonth(), dateB.getDate());

    return Math.floor((utc2 - utc1) / _MS_PER_DAY);
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

if (!Array.isArray) {
    Array.isArray = function(arg) {
        return Object.prototype.toString.call(arg) === '[object Array]';
    };
}
