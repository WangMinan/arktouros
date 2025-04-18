var myChart1 = echarts.init(document.getElementById('main1'));
var myChart2 = echarts.init(document.getElementById('main2'));
var myChart3 = echarts.init(document.getElementById('main3'));
var myChart5 = echarts.init(document.getElementById('main5'));


let backendUrl = ''
let arktourosUiIp = ''
init_config();

function init_config() {
	$.ajax({
		url: 'config.json',  // 指定 config.json 文件路径
		dataType: 'json',  // 指定返回的数据类型为 JSON
		success: function(config) {
			backendUrl = config.backendUrl;
			arktourosUiIp = config.arktourosUiIp;
			startBigScreen();
		},
		error: function(xhr, status, error) {
			console.error("Error loading config:", error);  // 处理错误
		}
	});
}

function navigateToUrl() {
	window.location.href = arktourosUiIp;
}

/**
 * 获取数据
 */
function startBigScreen() {
	updateNamespaceList();
}

function init_metric_charts(service) {
	let metricList = []
	$.ajax(backendUrl + '/metrics', {
		method: 'get',
		data: {
			serviceName: service,
			metricNameLimit: 2,
			// 一个月
			startTimeStamp: Date.now() - 30 * 24 * 3600 * 1000,
			endTimeStamp: Date.now(),
		},
		dataType: 'json',
		success: function (data) {
			if (data === null || !data.result) {
				return
			}
			if (data.result && data.result.length !== 0) {
				// 深拷贝data.result
				const tmpData = JSON.parse(JSON.stringify(data.result))
				tmpData.forEach(item => {
					item.metrics.forEach(metric => {
						metric.timestamp = timestampToJsTimeStr(metric.timestamp)
					})
				})
				metricList = tmpData
				init_metricCharts(metricList[0], 1)
				init_metricCharts(metricList[1], 3)
			}
		},
		error: function (data) {
			console.log(data)
		}
	})
}

/**
 * 服务资源饼状图
 */
function init_metricCharts(metric, index) {
	let option = getBasicMetricOption(metric, index)
	if (metric.metricType === 'GAUGE' || metric.metricType === 'COUNTER') {
		// 分情况 如果metric.metrics只有一个数据则使用仪表盘 否则使用线形图
		if (metric.metrics.length >= 2) {
			option.xAxis = {
				type: 'category',
				data: metric.metrics.map(item => item.timestamp)
			}
			option.yAxis = {
				type: 'value',
				name: getAxisTagName(metric.metrics[metric.metrics.length - 1]
					.value) === null ?
					null : 'Unit:' + getAxisTagName(metric.metrics[0].value),
				nameLocation: 'start',
				nameGap: 20
			}
			option.series = [{
				data: metric.metrics.map(item => {
					if (Number(item.value) > 1000000000) {
						// 转换成xG的格式
						return (Number(item.value) / 1000000000)
							.toFixed(2)
					} else if (Number(item.value) > 1000000) {
						// 转换成xM的格式
						return (Number(item.value) / 1000000)
							.toFixed(2)
					} else if (Number(item.value) > 1000) {
						// 转换成xK的格式
						return (Number(item.value) / 1000)
							.toFixed(2)
					} else {
						return item.value
					}
				}),
				type: 'line'
			}]
		} else {
			// 富文本
			option.series = [{
				type: 'scatter',
				symbolSize: 1,
				data: [
					{
						value: [0, 0],
						label: {
							show: true,
							formatter: metric.metrics[0].value + '',
							fontSize: 20,
							fontWeight: 'bold',
							// 红色字体 注意暗黑模式
							color: checkIsDark.value === 'dark' ? '#ff0000' : '#992233'
						}
					}],
			}]
			option.xAxis = {
				show: false,
				min: -1,
				max: 1
			}
			option.yAxis = {
				show: false,
				min: -1,
				max: 1
			}
		}
	} else if (metric.metricType === 'HISTOGRAM' || metric.metricType === 'SUMMARY') {
		let buckets = []
		// 遍历buckets
		for (const key in metric.metrics[0].buckets) {
			buckets.push({
				key: key,
				value: metric.metrics[0].buckets[key]
			})
		}
		// 按照key的数值大小对buckets进行排序
		buckets.sort((a, b) => a.key - b.key)
		option.xAxis = {
			type: 'category',
			data: buckets.map(item => item.key)
		}
		option.yAxis = {
			type: 'value',
			name: getAxisTagName(buckets[buckets.length - 1].value) === null ?
				null : 'Unit:' + getAxisTagName(buckets[buckets.length - 1].value),
		}
		option.series = [{
			data: buckets.map(item => {
				if (Number(item.value) > 1000000000) {
					// 转换成xG的格式
					return (Number(item.value) / 1000000000).toFixed(2)
				} else if (Number(item.value) > 1000000) {
					// 转换成xM的格式
					return (Number(item.value) / 1000000).toFixed(2)
				} else if (Number(item.value) > 1000) {
					// 转换成xK的格式
					return (Number(item.value) / 1000).toFixed(2)
				} else {
					return item.value
				}
			}),
			type: 'bar'
		}]
	}
	if (index === 1) {
		// 使用刚指定的配置项和数据显示图表。
		myChart1.setOption(option);
	} else if (index === 3) {
		myChart3.setOption(option);
	}
}

const getAxisTagName = (item) => {
	if (Number(item) > 1000000000) {
		// 转换成xG的格式
		return 'G'
	} else if (Number(item) > 1000000) {
		// 转换成xM的格式
		return 'M'
	} else if (Number(item) > 1000) {
		// 转换成xK的格式
		return 'K'
	} else {
		return null
	}
};

function getBasicMetricOption(metric, index) {
	return {
		backgroundColor: 'rgba(0,0,0,0)',
		title: {
			// 将metric.name中的下换线替换成空格
			text: metric.name.replace(/_/g, ' '),
			textStyle: {
				fontWeight: 'bold',
				fontSize: 14,
				lineHeight: 16,
				// 自动换行 获取dom元素宽度
				width: document.getElementById('main' + index).offsetWidth - 20,
				overflow: 'break', // 设置自动换行
				color: '#1bb4f9'
			},
			subtext: metric.metrics[0].description ? metric.metrics[0].description : '',
			subtextStyle: {
				fontSize: 10,
				lineHeight: 12,
				// 自动换行 获取dom元素宽度
				width: document.getElementById('main' + index).offsetWidth - 20,
				overflow: 'break', // 设置自动换行
				color: '#FFF'
			}
		},
		tooltip: {
			trigger: 'item', // 触发类型，可选值: 'item'（数据项触发），'axis'（坐标轴触发），'none'（不触发）
			axisPointer: {
				type: 'cross', // 设置触发提示的指示器类型
			},
			backgroundColor: 'rgba(83,93,105,0.8)',
			borderColor: '#535b69',
			textStyle: {
				color: '#FFF'
			},
		}
	};
}

/**
 * 服务占用资源实时展示
 */
function init_myChart2(namespace) {
	const nodes = []
	const calls = []

	$.ajax(backendUrl + '/service/topology', {
		method: 'get',
		data: {
			namespace: namespace
		},
		dataType: 'json',
		success: function (data) {
			response = data
			if (response.code === 2000) {
				for (item in response.result.nodes) {
					const node = response.result.nodes[item]
					nodes.push({
						draggable: true,
						name: node.nodeObject.name,
						category: node.nodeObject.status ? 0 : 1,
						symbolSize: [40, 40], // 关系图节点标记的大小，可以设置成诸如 10 这样单一的数字，也可以用数组分开表示宽和高，例如 [20, 10] 表示标记宽为20，高为10。
						item: node, // 传递给toolTip的附加信息
						itemStyle: {
							color: node.nodeObject.status ? "#C7EDCC" : "#FF2700",
						},
						label: {
							color: '#FFF'
						}
					})
				}
				for (item in response.result.calls) {
					const call = response.result.calls[item]
					if (call.source === null || call.target === null) {
						continue
					}
					calls.push({
						source: call.source.nodeObject.name,
						target: call.target.nodeObject.name
					})
				}
				drawServiceTopology(nodes, calls)
			} else {
				alert(`后端服务异常，获取${namespace}下节点拓扑异常，请重试`)
			}
		},
		error: function (data) {
			console.log(data)
		}
	})
}

const drawServiceTopology = (nodes, calls) => {
	let option = {
		backgroundColor: 'rgba(0,0,0,0)',
		legend: {
		}, // 图例
		tooltip: {
			trigger: 'item',
			triggerOn: 'mousemove',
			backgroundColor: 'rgba(83,93,105,0.8)',
			borderColor: '#535b69',
			textStyle: {
				color: '#FFF'
			},
			formatter: function (params) {
				// 通过修改SpanTreeNodeVo，我们把Span对象也放到params中
				return formatService(params.data.item.nodeObject);
			}
		},
		label: {                // 关系对象上的标签
			normal: {
				show: true,                 // 是否显示标签
				position: "inside",         // 标签位置:'top''left''right''bottom''inside''insideLeft''insideRight''insideTop''insideBottom''insideTopLeft''insideBottomLeft''insideTopRight''insideBottomRight'
			}
		},
		series: [
			{
				type: 'graph',
				edgeSymbol: ['none', 'arrow'],
				focusNodeAdjacency: true,   // 是否在鼠标移到节点上的时候突出显示节点以及节点的边和邻接节点。[ default: false ]
				roam: true, // 是否开启鼠标缩放和平移漫游。默认不开启。如果只想要开启缩放或者平移，可以设置成 'scale' 或者 'move'。设置成 true 为都开启
				layout: 'force', // 力引导布局 否则要手动指定xy坐标
				symbol: 'circle', // 标记的图形
				data: nodes,
				links: calls,
				label: { // 关系对象上的标签
					normal: {
						show: true, // 是否显示标签
						position: "inside", // 标签位置:'top''left''right''bottom''inside''insideLeft''insideRight''insideTop''insideBottom''insideTopLeft''insideBottomLeft''insideTopRight''insideBottomRight'
					}
				},
				force: {
					repulsion: 90, // 节点之间的斥力因子。[ default: 50 ]
					edgeLength: 90, // 边的两个节点之间的距离。这个距离也会受 repulsion 影响。[ default: 30 ]
					layoutAnimation: true, // 在每次迭代结束后开始一次布局更新的动画。[ default: false ]
				},
				animationEasingUpdate: "quinticInOut", // 数据更新动画的缓动效果。[ default: cubicOut ]    "quinticInOut"
				animationDurationUpdate: 100, // 数据更新动画的时长。[ default: 300 ]
				categories: [
					{
						name: '',
						itemStyle: {
							color: "#C7EDCC"
						}
					},
					{
						name: '',
						itemStyle: {
							color: "#FF2700"
						}
					}
				]
			}
		]
	}
	myChart2.setOption(option);
}

function formatService(service) {
	const status = service.status ? '正常' : '超时异常或离线'
	const tagsStr = service.tags.length === 0 ? '[]' : JSON.stringify(service.tags)

	return `<div>
				<div>
					<b>当前Service详细情况</b>
				</div>
				<ul>
					<li>id: ${service.id}</li>
					<li>名称: ${service.name}</li>
					<li>命名空间: ${service.namespace}</li>
					<li>延迟: ${service.latency} ms</li>
					<li>状态: ${status}</li>
					<li>标签: ${tagsStr}</li>
				</ul>
			</div>`;
}

/**
 * 容器运行属性展示
 */
function init_myChart4(service) {
	$.ajax(backendUrl + '/logs', {
		method: 'get',
		data: {
			pageNum: 1,
			pageSize: 10,
			serviceName: service,
			traceId: '',
			keyword: '',
			keywordNotIncluded: '',
			severityText: '',
			// startTimestamp: Date.now() - 3600000,
			startTimestamp: 649704600000,
			endTimestamp: Date.now()
		},
		dataType: 'json',
		success: function (data) {
			if (data.code === 2000) {
				let info = ''
				for (item in data.result.data) {
					let log = data.result.data[item]
					info = info + '<li><div>' + log.serviceName +
						'</div><div>' + timestampToJsTimeStr(log.timestamp) +
						'</div><div>' + log.severityText +
						'</div><div>' + log.content + '</div></li>'
				}
				if (info === '') {
					info = '<li>暂无日志</li>'
				}
				$('#log-info-ul').html(info)
			} else {
				alert('后端服务异常，获取日志列表失败，请重试')
			}
		},
		error: function (err) {
			console.log(err)
		}
	})
}

function init_myChart5(service) {
	let topology = {}
	var interval = null

	$.ajax(backendUrl + '/trace/endPoints', {
		method: 'get',
		data: {
			serviceName: service,
			pageNum: 1,
			pageSize: 10
		},
		dataType: 'json',
		success: function (data) {
			if (data.code === 2000) {
				const traceIdList = []
				data.result.forEach(item => {
					item.traceIds.forEach(traceId => {
						traceIdList.push(traceId)
					})
				})
				if (traceIdList.length === 0) {
					return
				}
				// 直接选第一个
				$.ajax(backendUrl + '/trace/topology', {
					method: 'get',
					data: {
						traceId: traceIdList[0],
						serviceName: service,
						innerService: false
					},
					dataType: 'json',
					success: function (data) {
						if (data === null) {
							return
						}
						if (data.code === 2000) {
							topology = data.result
							let option = {
								title: {
									subtext: '绿色为正常Span节点，红色为异常Span节点',
									align: 'right'
								},
								backgroundColor: 'rgba(0,0,0,0)',
								borderColor: '#535b69',
								tooltip: {
									trigger: 'item',
									triggerOn: 'mousemove',
									backgroundColor: 'rgba(83,93,105,0.8)',
									borderColor: '#535b69',
									textStyle: {
										color: '#FFF'
									},
									// 自定义提示框内容的回调函数 params参数实际存储的就是SpanTreeNodeVo对象
									formatter: function (params) {
										// 通过修改SpanTreeNodeVo，我们把Span对象也放到params中
										return formatSpan(params.data.span);
									}
								},
								series: [
									{
										type: 'tree',
										symbol: 'circle', // 标记的图形
										roam: true,//移动+放大
										expandAndCollapse: true,
										animationDuration: 550,
										animationDurationUpdate: 750,
										label: {
											position: 'right',
											verticalAlign: 'middle',
											fontSize: 9
										},
										initialTreeDepth: -1,
										data: [topology]
									}
								]
							}
							myChart5.setOption(option);
						} else {
							alert('后端服务异常，获取链路拓扑图失败，请重试')
						}
					},
					error: function (xhr, status, error) {
						console.log(error)
					}
				})

			} else {
				alert('后端服务异常，获取链路拓扑图失败，请重试')
			}
		},
		error: function (xhr, status, error) {
			console.log(error)
		}
	})
}

function formatToTargetLengthDigits(str, length) {
	// 如果字符串长度超过16位，则截断
	if (str.length > length) {
		return str.slice(0, length);
	}

	// 如果字符串长度不足16位，则在后面补0
	return str.padEnd(length, '0');
}

const // 时间戳：1637244864707
	/* 时间戳转换为时间 */
	timestampToJsTimeStr = (timestamp) => {
		if (timestamp === '0') {
			return 'unknown'
		}
		// 历史遗留问题 将timestamp调节到13位 多删少补
		timestamp = formatToTargetLengthDigits(timestamp, 13)

		timestamp = Number(timestamp)

		let date = new Date(timestamp);
		let Y = date.getFullYear() + '-';
		let M = (date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1) + '-';
		let D = (date.getDate() < 10 ? '0' + date.getDate() : date.getDate()) + ' ';
		let h = (date.getHours() < 10 ? '0' + date.getHours() : date.getHours()) + ':';
		let m = (date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()) + ':';
		let s = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();
		return Y + M + D + h + m + s;
	}

function formatSpan(span) {
	const status = span.endTime === '-1' ? '超时异常或离线' : '正常'
	const startTime = timestampToJsTimeStr(span.startTime)
	const endTime = span.endTime === '-1' ? '该Span异常' : timestampToJsTimeStr(span.endTime)
	const localIp = span.localEndPoint.ip === '' ? 'null' : span.localEndPoint.ip
	const remoteIp = span.remoteEndPoint.ip === '' ? 'null' : span.remoteEndPoint.ip
	return `<div>
				<div>
					<b>当前Span详细情况</b>
				</div>
				<ul>
					<li>id: ${span.id}</li>
					<li>名称: ${span.name}</li>
					<li>所属服务: ${span.serviceName}</li>
					<li>开始时间: ${startTime}</li>
					<li>结束时间: ${endTime}</li>
					<li>span状态: ${status}</li>
					<li>父节点SpanId: ${span.parentSpanId}</li>
					<li>所属endPoint: ${span.localEndPoint.serviceName}</li>
					<li>所属endPoint ip与端口: ${localIp}:${span.localEndPoint.port}</li>
					<li>远程endPoint: ${span.remoteEndPoint.serviceName}</li>
					<li>远程endPoint ip与端口: ${remoteIp}:${span.remoteEndPoint.port}</li>
				</ul>
			</div>`;
}

//获取当前时间
function getNowFormatDate() {
	var date = new Date();
	var year = date.getFullYear();
	var month = date.getMonth() + 1;
	var strDate = date.getDate();
	var Hour = date.getHours();       // 获取当前小时数(0-23)
	var Minute = date.getMinutes();     // 获取当前分钟数(0-59)
	var Second = date.getSeconds();     // 获取当前秒数(0-59)
	var show_day = new Array('星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六');
	var day = date.getDay();
	if (Hour < 10) {
		Hour = "0" + Hour;
	}
	if (Minute < 10) {
		Minute = "0" + Minute;
	}
	if (Second < 10) {
		Second = "0" + Second;
	}
	if (month >= 1 && month <= 9) {
		month = "0" + month;
	}
	if (strDate >= 0 && strDate <= 9) {
		strDate = "0" + strDate;
	}
	var currentdate = '<div><p>' + year + '年' + month + '月' + strDate + '号</p><p>' + show_day[day] + '</p></div>';
	var HMS = Hour + ':' + Minute + ':' + Second;
	var temp_time = year + '-' + month + '-' + strDate + ' ' + HMS;
	$('.nowTime li:nth-child(1)').html(HMS);
	$('.nowTime li:nth-child(2)').html(currentdate);
	//$('.topRec_List li div:nth-child(3)').html(temp_time);
	setTimeout(getNowFormatDate, 1000);//每隔1秒重新调用一次该函数
}

/**
 * 更新namespace列表
 */
function updateNamespaceList() {
	let response = {}

	$.ajax(backendUrl + '/service/namespaces', {
		method: 'get',
		data: {},
		dataType: 'json',
		success: function (data) {
			response = data
			if (response.code === 2000) {
				let options = ''
				for (item in response.result) {
					options += '<option value="' + response.result[item] + '">' + response.result[item] + '</option>'
				}
				$('#namespace-select').html(options)
				let namespace = $('#namespace-select option:selected').text()
				init_myChart2(namespace);
				updateSeviceList(namespace);
			} else {
				alert('后端服务异常，获取命名空间列表失败，请重试')
			}
		},
		error: function (data) {
			console.log(data)
		}
	})
}

/**下拉框点击事件 */
$('#namespace-select').change(function (e) {
	let namespace = $('#namespace-select option:selected').text()
	init_myChart2(namespace);
	updateSeviceList(namespace);
})

/**
 * 更新namespace列表
 */
function updateSeviceList(namespace) {
	let response = {}

	$.ajax(backendUrl + '/services', {
		method: 'get',
		data: {
			pageNum: 1,
			pageSize: 30,
			namespace: namespace,
			query: ''
		},
		dataType: 'json',
		success: function (data) {
			response = data
			if (response.code === 2000) {
				let options = ''
				for (item in response.result.data) {
					options += '<option value="' + response.result.data[item].name + '">' +
						response.result.data[item].name + '</option>'
				}
				$('#service-select').html(options)
				let service = $('#service-select option:selected').text()
				init_metric_charts(service)
				init_myChart4(service)
				init_myChart5(service)
			} else {
				alert('后端服务异常，获取服务名称列表失败，请重试')
			}
		},
		error: function (data) {
			console.log(data)
		}
	})
}

/**下拉框点击事件 */
$('#service-select').change(function (e) {
	let service = $('#service-select option:selected').text()
	init_metric_charts(service)
	init_myChart4(service)
	init_myChart5(service)
})

setInterval(function () {
	window.onresize = function () {
		this.myChart1.resize;
		this.myChart2.resize;
		this.myChart3.resize;
		this.myChart5.resize;
	}
}, 200)

