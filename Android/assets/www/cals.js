
alert("cals.js");
console.log("asdfasdfasdf");

function sum(a,b){
	alert(a+b);
}

onmessage = function(evt){
	
	//test();
	//alert("test1");
	for(var i=evt; i < 10000; i++)
	{
		//if((i%10000) == 0) postMessage(i);
		console.log("workerthread : " + i);
	};
	
};

function test(){
	alert("in test");
}