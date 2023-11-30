export function toGraph(series,xaxis,yaxis) {
    let graph = series.map(
        (num,index) => {
            return({[xaxis]:index,[yaxis]:num});
        }
    );
    return graph;
}

export function toBarGraph(data,xaxis="name",yaxis="value") {
    // data is of the format
    // {
    //     name1: value1
    //     name2: value2
    // }
    console.log(data);
    let graph = Object.keys(data).map(
        key => ({name:key,[yaxis]:data[key]})
    );
    console.log(graph);
    return graph;

}