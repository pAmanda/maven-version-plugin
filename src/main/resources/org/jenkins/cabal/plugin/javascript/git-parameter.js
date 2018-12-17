function createBranchValues(arr) {
    createValues(arr, "branchSelect")
}

function createTagValues(arr) {
    createValues(arr, "tagSelect")
}

function createValues(arr, selectId) {
    if((typeof arr == 'string' && arr.trim().length > 0) || typeof arr != 'undefined' || arr != null) {
        var values = arr.trim().replace("[", "").replace("]", "").split(",");

        var select = document.getElementById(selectId)
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
    var e = document.getElementById("environment");
    var itemSelecionado = e.options[e.selectedIndex].text;
    console.log(itemSelecionado)
    if(itemSelecionado == "Staging") {
        document.getElementById("version").style.display = "block";
        document.getElementById("branch").style.display = "block";
        document.getElementById("tag").style.display = "none";
    } else if (itemSelecionado == "Production"){
        document.getElementById("version").style.display = "none";
        document.getElementById("branch").style.display = "none";
        document.getElementById("tag").style.display = "block";
    } else {
        document.getElementById("version").style.display = "block";
        document.getElementById("branch").style.display = "block";
        document.getElementById("tag").style.display = "block";
    }
}

