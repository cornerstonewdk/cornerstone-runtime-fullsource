var PageLoading = function() {};

PageLoading.prototype.getStartPageLoadingTime = function(successcallback,errorcallback) {
	srt.exec(successcallback, errorcallback, 'PageLoading', 'getStartPageLoadingTime', []);
};

PageLoading.prototype.getEndPageLoadingTime = function(successcallback,errorcallback) {
	srt.exec(successcallback, errorcallback, 'PageLoading', 'getEndPageLoadingTime', []);
};

PageLoading.prototype.getPageLoadingTime = function(successcallback,errorcallback) {
	srt.exec(successcallback, errorcallback, 'PageLoading', 'getPageLoadingTime', []);
};

PageLoading.prototype.getLoadDataUrl = function(successcallback,errorcallback) {
	srt.exec(successcallback, errorcallback, 'PageLoading', 'getLoadDataUrl', []);
};

srt.addConstructor(function() {
	srt.addPlugin('PageLoading', new PageLoading());
});