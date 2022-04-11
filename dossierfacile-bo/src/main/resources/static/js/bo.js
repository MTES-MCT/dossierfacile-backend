$(document).ready(function () {

    setTimeout(function() {
        $('#successMessage').fadeOut('fast');
    }, 5000); // <-- time in milliseconds

    var table = $('#propertiesTable').DataTable({
        "sAjaxSource": "/bo/properties-pro/table",
        "sAjaxDataProp": "",
        "order": [[5, "desc"]],
        "aoColumns": [
            {
                "mRender": function (data, type, row) {
                    if (row.floating) {
                        return '<span class="badge" style = "background-color: red;color: red">a</span>'
                    } else {
                        return '';
                    }
                }
            },
            {"mData": "name"},
            {"mData": "propertyId"},
            {"mData": "rentCost"},
            {"mData": "agentEmail"},
            {"mData": "dateFormat"},
            {"mData": "cantProspect"},
            {
                "mRender": function (data, type, row) {
                    var buttons = '<a class="fa fa-pencil nounderline" href="#" ' +
                        'data-toggle="modal" ' +
                        'data-target="#editProperty"' +
                        'data-name="' + row.name + '"' +
                        'data-agent-email="' + row.agentEmail + '"' +
                        'data-property-id="' + row.propertyId + '"' +
                        'data-rent-cost="' + row.rentCost + '"' +
                        'data-id="' + row.id + '"' +
                        '></a> ' +
                        '<a class="fa fa-trash nounderline" href="#" ' +
                        'data-toggle="modal" ' +
                        'data-target="#deleteProperty"' +
                        'data-id="' + row.id + '"' +
                        '></a>';
                    if (row.displayed) {
                        buttons += ' <a class="fa fa-eye-slash nounderline" href="/bo/properties-pro/display/' + row.id + '"></a>'
                    } else {
                        buttons += ' <a class="fa fa-eye nounderline" href="/bo/properties-pro/display/' + row.id + '"></a>'
                    }
                    return buttons;
                }
            }
        ]
    });
    $(function() {
        setTimeout(function() { $("#hideDiv").fadeOut(1500); }, 4000)

    })
    $('#apartmentSharingTable').DataTable({
        "processing": true,
        "serverSide": true,
        "pageLength": 10,
        "searching": false,
        "sort": false,
        "info": true,
        "ajax": {
            "url": "/bo/colocation/table",
            "method": "get",
            "dataSrc": function (response) {

                var data = response.data; // your data list

                var all = [];

                for (var i = 0; i < data.length; i++) {


                    var row = {

                        rows: response.start + i + 1,
                        id: data[i].id,
                        userApiId: data[i].userApiId,
                        partnerId: data[i].partnerId,
                        creationDate: data[i].creationDate,
                        firstName: data[i].firstName,
                        lastName: data[i].lastName,
                        numberOfTenants: data[i].numberOfTenants,
                        numberOfCompleteRegister: data[i].numberOfCompleteRegister,
                        status: data[i].status,

                    };
                    all.push(row);
                }
                return all;
            }

        },
        "columns": [
            {
                "mRender": function (data, type, row) {
                    var show = '<span>' + row.id + '</span><br/>';
                    if (row.userApiId != null) {
                        if (row.partnerId != null) {
                            show += '<span class="label label-info">' + row.partnerId + '</span>'
                        } else {
                            show += '<span class="label label-info">api</span>'
                        }
                    }
                    return show;
                }
            },
            {"data": "creationDate"},
            {"data": "firstName"},
            {"data": "lastName"},
            {"data": "numberOfTenants"},
            {"data": "numberOfCompleteRegister"},
            {"data": "status"},
            {
                "mRender": function (data, type, row) {
                    return '<a class="fa fa-eye nounderline"' +
                        'href="/bo/colocation/' + row.id + '">';
                }
            }
        ]
    });


});

