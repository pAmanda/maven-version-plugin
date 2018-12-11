function createBranchValues(arr) {

    if((typeof arr == 'string' && arr.trim().length > 0) || typeof arr != 'undefined' || arr != null) {
        var values = arr.trim().replace("[", "").replace("]", "").split(",");

        var select = document.getElementById("gitParameterSelect")
        for(var i = 0; i < values.length; i++)
        {
            var option = document.createElement("OPTION"),
                txt = document.createTextNode(values[i]);
            option.appendChild(txt);
            option.setAttribute("value",values[i]);
            select.insertBefore(option,select.lastChild);
        }
    }

}

function showHide() {
    var e = document.getElementById("typeDeploy");
    var itemSelecionado = e.options[e.selectedIndex].text;
    console.log(itemSelecionado)

}
