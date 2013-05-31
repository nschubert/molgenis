(function($, w) {
	"use strict";

	var ns = w.molgenis = w.molgenis || {};

	var resultsTable = null;
	var featureFilters = {};
	var selectedFeatures = [];
	var searchQuery = null;
	var selectedDataSet = null;
	var currentPage = 1;
	var restApi = new ns.RestClient();
	var searchApi = new ns.SearchClient();

	// fill dataset select
	ns.fillDataSetSelect = function(callback) {
		restApi.getAsync('/api/v1/dataset', null, null, function(datasets) {
			var items = [];
			// TODO deal with multiple entity pages
			$.each(datasets.items, function(key, val) {
				items.push('<option value="' + val.href + '">' + val.name + '</option>');
			});
			$('#dataset-select').html(items.join(''));
			$('#dataset-select').change(function() {
				ns.onDataSetSelectionChange($(this).val());
			});
			callback();
		});
	};

	ns.createFeatureSelection = function(protocolUri) {
		function createChildren(protocolUri, featureOpts, protocolOpts) {
			var protocol = restApi.get(protocolUri, [ "features", "subprotocols" ]);

			var children = [];
			if (protocol.subprotocols) {
				// TODO deal with multiple entity pages
				$.each(protocol.subprotocols.items, function() {
					children.push($.extend({
						key : this.href,
						title : this.name,
						tooltip : this.description,
						isFolder : true,
						isLazy : protocolOpts.expand != true,
						children : protocolOpts.expand ? createChildren(this.href, featureOpts, protocolOpts) : null
					}, protocolOpts));
				});
			}
			if (protocol.features) {
				// TODO deal with multiple entity pages
				$.each(protocol.features.items, function() {
					children.push($.extend({
						key : this.href,
						title : this.name,
						tooltip : this.description,
						icon : "../../img/filter-bw.png",
					}, featureOpts));
				});
			}
			return children;
		}

		function expandNodeRec(node) {
			if (node.childList == undefined) {
				node.toggleExpand();
			} else {
				$.each(node.childList, function() {
					expandNodeRec(this);
				});
			}
		}

		function onNodeSelectionChange(selectedNodes) {
			function getSiblingPos(node) {
				var pos = 0;
				do {
					node = node.getPrevSibling();
					if (node == null)
						break;
					else
						++pos;
				} while (true);
				return pos;
			}
			var sortedNodes = selectedNodes.sort(function(node1, node2) {
				var diff = node1.getLevel() - node2.getLevel();
				if (diff == 0) {
					diff = getSiblingPos(node1.getParent()) - getSiblingPos(node2.getParent());
					if (diff == 0)
						diff = getSiblingPos(node1) - getSiblingPos(node2);
				}
				return diff <= 0 ? -1 : 1;
			});
			var sortedFeatures = $.map(sortedNodes, function(node) {
				return node.data.isFolder ? null : node.data.key;
			});
			ns.onFeatureSelectionChange(sortedFeatures);
		}

		restApi.getAsync(protocolUri, [ "features", "subprotocols" ], null, function(protocol) {
			var container = $('#feature-selection');
			if (container.children('ul').length > 0) {
				container.dynatree('destroy');
			}
			container.empty();
			if (typeof protocol === 'undefined') {
				container.append("<p>No features available</p>");
				return;
			}

			// render tree and open first branch
			container.dynatree({
				checkbox : true,
				selectMode : 3,
				minExpandLevel : 2,
				debugLevel : 0,
				children : [ {
					key : protocol.href,
					title : protocol.name,
					icon : false,
					isFolder : true,
					isLazy : true,
					children : createChildren(protocol.href, {
						select : true
					}, {})
				} ],
				onLazyRead : function(node) {
					// workaround for dynatree lazy parent node select bug
					var opts = node.isSelected() ? {
						expand : true,
						select : true
					} : {};
					var children = createChildren(node.data.key, opts, opts);
					node.setLazyNodeStatus(DTNodeStatus_Ok);
					node.addChild(children);
				},
				onClick : function(node, event) {
					if ((node.getEventTargetType(event) === "title" || node.getEventTargetType(event) === "icon") && !node.data.isFolder)
						ns.openFeatureFilterDialog(node.data.key);
				},
				onSelect : function(select, node) {
					// workaround for dynatree lazy parent node select bug
					if (select)
						expandNodeRec(node);
					onNodeSelectionChange(this.getSelectedNodes());
				},
				onPostInit : function() {
					onNodeSelectionChange(this.getSelectedNodes());
				}
			});
		});
	};

	ns.onDataSetSelectionChange = function(dataSetUri) {
		// reset
		featureFilters = {};
		$('#feature-filters p').remove();
		selectedFeatures = [];
		searchQuery = null;
		resultsTable.resetSortRule();
		currentPage = 1;
		$("#observationset-search").val("");

		restApi.getAsync(dataSetUri, null, null, function(dataSet) {
			selectedDataSet = dataSet;
			ns.createFeatureSelection(dataSet.protocolUsed.href);
		});
	};

	ns.onFeatureSelectionChange = function(featureUris) {
		selectedFeatures = featureUris;
		ns.updateObservationSetsTable();
	};

	ns.searchObservationSets = function(query) {
		console.log("searchObservationSets: " + query);

		// Reset
		resultsTable.resetSortRule();
		currentPage = 1;

		searchQuery = query;
		ns.updateObservationSetsTable();
	};
	
	ns.searchFeatureTable = function(query, protocolUri) {
		
		function getEntitiesbyIds(map, entityName){	
			var array = Object.keys(map); 
			var iteration = Math.floor(array.length / 100);
			for(var i = 1; i <= iteration; i++ ){
				callRestApi(map, array.slice((i - 1) * 100, i * 100), entityName);
			}
			if(iteration * 100 < array.length){
				callRestApi(map, array.slice(iteration * 100, array.length), entityName);
			}
		}
		
		function callRestApi(map, array, entityName){
			$.ajax({
				type : 'POST',
				url : '/api/v1/' + entityName + '?_method=GET',
				data : JSON.stringify({
					q : [ {
						"field" : "id",
						"operator" : "IN",
						"value" : array
					} ],
					num : array.length
				}),
				contentType : 'application/json',
				async : false,
				success : function(entities) {
					$.each(entities.items, function() {
						var object = $(this)[0];
						var fragments = object.href.split("/");
						var id = fragments[fragments.length - 1];
						map[id] = object;
					});
				}
			});
		}
		
		console.log("searchObservationSets: " + query);
		searchQuery = query;
		ns.searchFeatureMeta(function(searchResponse) {
			var protocol = restApi.get(protocolUri);
			var rootNode =  $('#feature-selection').dynatree("getTree").getNodeByKey(protocol.href);
			rootNode.removeChildren();
			var searchHits = searchResponse["searchHits"];
			
			var protocolMap = {};
			var featureMap = {};
			$.each(searchHits, function(){
				var object = $(this)[0]["columnValueMap"];
				var entityType = object["type"];
				var nodes = object["path"].split(".");
				var entityId = object["id"];

				//collect all features and their ancesters using restapi first.
				if(entityType === "observablefeature"){
					for(var i = 0; i < nodes.length; i++){
						if(nodes[i] == entityId.toString()){
							featureMap[nodes[i]] = nodes[i];
						}else{
							protocolMap[nodes[i]] = nodes[i];
						}
					}
				}
			});
			getEntitiesbyIds(protocolMap, "protocol");
			getEntitiesbyIds(featureMap, "observablefeature");
			
			var cachedNode = {};
			var topNodes = new Array();
			
			$.each(searchHits, function(){
				var object = $(this)[0]["columnValueMap"];
				var entityType = object["type"];
				
				if(entityType === "observablefeature"){
					
					var nodes = object["path"].split(".");
					var entityId = object["id"];
					//split the path to get all ancestors;
					for(var i = 0; i < nodes.length; i++) {
						if(!cachedNode[nodes[i]]){
							var entityInfo = null;
							var options = null;
							//this is the last node and check if this is a feature
							if (nodes[i] === entityId.toString()) {
								entityInfo = featureMap[nodes[i]];
								options = {
									isFolder : false,
									icon : "../../img/filter-bw.png"
								}
							}else{
								entityInfo = protocolMap[nodes[i]];
								options = {
									isFolder : true,
									isLazy : true,
									expand : true,
									children : []
								};
							}
							options = $.extend({
								key : entityInfo.href,
								title : entityInfo.name,
								tooltip : entityInfo.description
							}, options);
							//locate the node in dynatree and otherwise create the node and insert it
							if(i != 0){
								var parentNode = cachedNode[nodes[i-1]];
								parentNode["children"].push(options);
								cachedNode[nodes[i-1]] = parentNode;
							}
							else
								topNodes.push(options);
							cachedNode[nodes[i]] = options;
						}
					}
				}
			});
			rootNode.addChild(topNodes);
			console.log("finished");
		});
	}

	ns.updateObservationSetsTable = function() {
		if (selectedFeatures.length > 0) {
			ns.search(function(searchResponse) {
				var maxRowsPerPage = resultsTable.getMaxRows();
				var nrRows = searchResponse.totalHitCount;

				resultsTable.build(searchResponse, selectedFeatures, restApi);

				ns.onObservationSetsTableChange(nrRows, maxRowsPerPage);
			});
		} else {
			$('#data-table-header').html('no features selected');
			$('#data-table-pager').empty();
			$('#data-table').empty();
		}
	};

	ns.onObservationSetsTableChange = function(nrRows, maxRowsPerPage) {
		console.log("onObservationSetsTableChange");
		ns.updateObservationSetsTablePager(nrRows, maxRowsPerPage);
		ns.updateObservationSetsTableHeader(nrRows);
	};

	ns.updateObservationSetsTableHeader = function(nrRows) {
		console.log("updateObservationSetsTableHeader");
		if (nrRows == 1)
			$('#data-table-header').html(nrRows + ' data item found');
		else
			$('#data-table-header').html(nrRows + ' data items found');
	};

	ns.updateObservationSetsTablePager = function(nrRows, nrRowsPerPage) {
		console.log("updateObservationSetsTablePager");
		$('#data-table-pager').empty();
		var nrPages = Math.ceil(nrRows / nrRowsPerPage);
		if (nrPages <= 1)
			return;

		var pager = $('#data-table-pager');
		var ul = $('<ul>');
		pager.append(ul);

		if (currentPage == 1) {
			ul.append($('<li class="disabled"><span>&laquo;</span></li>'));
		} else {
			var prev = $('<li><a href="#">&laquo;</a></li>');
			prev.click(function(e) {
				currentPage--;
				ns.updateObservationSetsTable();
				return false;
			});
			ul.append(prev);
		}

		for ( var i = 1; i <= nrPages; ++i) {
			if (i == currentPage) {
				ul.append($('<li class="active"><span>' + i + '</span></li>'));

			} else if ((i == 1) || (i == nrPages) || ((i > currentPage - 3) && (i < currentPage + 3)) || ((i < 7) && (currentPage < 5))
					|| ((i > nrPages - 6) && (currentPage > nrPages - 4))) {

				var p = $('<li><a href="#">' + i + '</a></li>');
				p.click((function(pageNr) {
					return function() {
						currentPage = pageNr;
						ns.updateObservationSetsTable();
						return false;
					};
				})(i));

				ul.append(p);
			} else if ((i == 2) || (i == nrPages - 1)) {
				ul.append($('<li class="disabled"><span>...</span></li>'));

			}
		}

		if (currentPage == nrPages) {
			ul.append($('<li class="disabled"><span>&raquo;</span></li>'));
		} else {
			var next = $('<li><a href="#">&raquo;</a></li>');
			next.click(function() {
				currentPage++;
				ns.updateObservationSetsTable();
				return false;
			});
			ul.append(next);
		}

		pager.append($('</ul>'));
	};

	ns.openFeatureFilterDialog = function(featureUri) {
		console.log("openFeatureFilterDialog: " + featureUri);
		restApi.getAsync(featureUri, null, null, function(feature) {
			var items = [];
			if (feature.description)
				items.push('<h3>Description</h3><p>' + feature.description + '</p>');
			items.push('<h3>Value (' + feature.dataType + ')</h3>');
			var filter = null;
			var config = featureFilters[featureUri];

			switch (feature.dataType) {
			case "xref":
			case "string":
				if (config == null)
					filter = $('<input type="text" placeholder="filter text" autofocus="autofocus">');
				else
					filter = $('<input type="text" placeholder="filter text" autofocus="autofocus" value="' + config.values[0] + '">');
				filter.change(function() {
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ $(this).val() ]
					});
				});
				break;
			case "date":
				if (config == null)
					filter = $('<input type="date" autofocus="autofocus">');
				else
					filter = $('<input type="date" autofocus="autofocus" value="' + config.values[0] + '">');
				filter.change(function() {
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ $(this).val() ]
					});
				});
				break;
			case "datetime":
				if (config == null)
					filter = $('<input type="datetime-local" autofocus="autofocus">');
				else
					filter = $('<input type="datetime-local" autofocus="autofocus" value="' + config.values[0] + '">');
				filter.change(function() {
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ $(this).val() ]
					});
				});
				break;
			case "integer":
			case "int":
			case "decimal":
				var fromFilter;
				if (config == null)
					fromFilter = $('<input id="from" type="number" autofocus="autofocus" step="any">');
				else
					fromFilter = $('<input id="from" type="number" autofocus="autofocus" step="any" value="' + config.values[0] + '">');

				fromFilter.change(function() {
					// If 'from' changed set 'to' at the same value
					var value = $('#from').val();
					$('#to').val(value);
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ value, value ],
						range : true
					});
				});

				var toFilter;
				if (config == null)
					toFilter = $('<input id="to" type="number" autofocus="autofocus" step="any">');
				else
					toFilter = $('<input id="to" type="number" autofocus="autofocus" step="any" value="' + config.values[1] + '">');

				toFilter.change(function() {
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ $('#from').val(), $('#to').val() ],
						range : true
					});
				});

				filter = $('<span>From:<span>').after(fromFilter).after($('<span>To:</span>')).after(toFilter);
				break;
			case "bool":
				if (config == null)
					filter = $('<input type="checkbox" autofocus="autofocus">');
				else
					filter = $('<input type="checkbox" autofocus="autofocus" value="' + config.values[0] + '">');
				filter.change(function() {
					ns.updateFeatureFilter(featureUri, {
						name : feature.name,
						identifier : feature.identifier,
						type : feature.dataType,
						values : [ $(this).val() ]
					});
				});
				break;
			case "categorical":
				$.ajax({
					type : 'POST',
					url : '/api/v1/category?_method=GET',
					data : JSON.stringify({
						q : [ {
							"field" : "observableFeature_Identifier",
							"operator" : "EQUALS",
							"value" : feature.identifier
						} ]
					}),
					contentType : 'application/json',
					async : false,
					success : function(categories) {
						filter = [];
						$.each(categories.items, function() {
							var input;
							if (config && ($.inArray(this.name, config.values) > -1)) {
								input = $('<input type="checkbox" name="' + feature.identifier + '" value="' + this.name + '" checked>');
							} else {
								input = $('<input type="checkbox" name="' + feature.identifier + '" value="' + this.name + '">');
							}

							input.change(function() {
								ns.updateFeatureFilter(featureUri, {
									name : feature.name,
									identifier : feature.identifier,
									type : feature.dataType,
									values : $.makeArray($('input[name="' + feature.identifier + '"]:checked').map(function() {
										return $(this).val();
									}))
								});
							});
							filter.push($('<label class="checkbox">').html(' ' + this.name).prepend(input));
						});
					}
				});
				break;
			case "nominal":
			case "ordinal":
			case "code":
			case "image":
			case "file":
			case "log":
			case "data":
			case "exe":
				console.log("TODO: '" + feature.dataType + "' not supported");
				break;
			}

			$('.feature-filter-dialog').html(items.join('')).append(filter);
			$('.feature-filter-dialog').dialog({
				title : feature.name,
				dialogClass : 'ui-dialog-shadow'
			});
			$('.feature-filter-dialog').dialog('open');
		});
	};

	ns.updateFeatureFilter = function(featureUri, featureFilter) {
		featureFilters[featureUri] = featureFilter;
		ns.onFeatureFilterChange(featureFilters);
	};

	ns.removeFeatureFilter = function(featureUri) {
		delete featureFilters[featureUri];
		ns.onFeatureFilterChange(featureFilters);
	};

	ns.onFeatureFilterChange = function(featureFilters) {
		ns.createFeatureFilterList(featureFilters);
		ns.updateObservationSetsTable();
	};

	ns.createFeatureFilterList = function(featureFilters) {
		var items = [];
		$.each(featureFilters, function(featureUri, feature) {
			items.push('<p><a class="feature-filter-edit" data-href="' + featureUri + '" href="#">' + feature.name + ' ('
					+ feature.values.join(',') + ')</a><a class="feature-filter-remove" data-href="' + featureUri
					+ '" href="#" title="Remove ' + feature.name + ' filter" ><i class="ui-icon ui-icon-closethick"></i></a></p>');
		});
		items.push('</div>');
		$('#feature-filters').html(items.join(''));

		$('.feature-filter-edit').click(function() {
			ns.openFeatureFilterDialog($(this).data('href'));
			return false;
		});
		$('.feature-filter-remove').click(function() {
			ns.removeFeatureFilter($(this).data('href'));
			return false;
		});
	};

	ns.search = function(callback) {
		searchApi.search(ns.createSearchRequest(), callback);
	};
	
	ns.searchFeatureMeta = function(callback){
		searchApi.search(ns.createSearchRequestFeatureMeta(), callback);
	}
	
	ns.createSearchRequestFeatureMeta = function(){
		var terms = searchQuery.split(" ");
		var queryRules = new Array();
		$.each(terms, function(index, element){
			queryRules.push({
				operator : 'SEARCH',
				value : element,
			});
			if(index < terms.length - 1)
				queryRules.push({
					operator : 'AND'
				});
		});
		//todo: how to unlimit the search result
		queryRules.push({
			operator : 'LIMIT',
			value : 1000000
		});
		var searchRequest = {
			documentType : "protocolViewer-" + selectedDataSet.name,
			queryRules : queryRules
		};
		return searchRequest;
	}
	
	ns.createSearchRequest = function() {
		var searchRequest = {
			documentType : selectedDataSet.name,
			queryRules : [ {
				operator : 'LIMIT',
				value : resultsTable.getMaxRows()
			} ]
		};

		if (currentPage > 1) {
			var offset = (currentPage - 1) * resultsTable.getMaxRows();
			searchRequest.queryRules.push({
				operator : 'OFFSET',
				value : offset
			});
		}

		var count = 0;

		if (searchQuery) {
			searchRequest.queryRules.push({
				operator : 'SEARCH',
				value : searchQuery
			});
			count++;
		}

		$.each(featureFilters, function(featureUri, filter) {
			if (count > 0) {
				searchRequest.queryRules.push({
					operator : 'AND'
				});
			}
			$.each(filter.values, function(index, value) {
				if (filter.range) {
					// Range filter
					var rangeAnd = false;
					if ((index == 0) && (value != '')) {
						searchRequest.queryRules.push({
							field : filter.identifier,
							operator : 'GREATER_EQUAL',
							value : value
						});
						rangeAnd = true;
					}
					if (rangeAnd) {
						searchRequest.queryRules.push({
							operator : 'AND'
						});
					}
					if ((index == 1) && (value != '')) {
						searchRequest.queryRules.push({
							field : filter.identifier,
							operator : 'LESS_EQUAL',
							value : value
						});
					}

				} else {
					if (index > 0) {
						searchRequest.queryRules.push({
							operator : 'OR'
						});
					}
					searchRequest.queryRules.push({
						field : filter.identifier,
						operator : 'EQUALS',
						value : value
					});
				}

			});

			count++;
		});

		var sortRule = resultsTable.getSortRule();
		if (sortRule) {
			searchRequest.queryRules.push(sortRule);
		}

		searchRequest.fieldsToReturn = [];
		$.each(selectedFeatures, function() {
			var feature = restApi.get(this);
			searchRequest.fieldsToReturn.push(feature.identifier);
		});

		return searchRequest;
	};

	// on document ready
	$(function() {
		resultsTable = new ns.ResultsTable();

		$("#observationset-search").focus();
		$("#observationset-search").change(function(e) {
			ns.searchObservationSets($(this).val());
		});
		$("#feature-search").keyup(function(e){
				e.preventDefault();
		    if(e.keyCode == 13 || e.which === '13') // enter
		        {$("#search-feature-button").click();}
		});
		$("#search-feature-button").click(function(e) {
			if($("#feature-search").val() != "" && $("#feature-search").val() != undefined){
				ns.searchFeatureTable($("#feature-search").val(), selectedDataSet.protocolUsed.href);
			}
		});
		$("#search-feature-clear-button").click(function(e) {
			ns.createFeatureSelection(selectedDataSet.protocolUsed.href);
			$("#feature-search").val("");
		});
		$('.feature-filter-dialog').dialog({
			modal : true,
			width : 500,
			autoOpen : false
		});
	});
}($, window.top));
