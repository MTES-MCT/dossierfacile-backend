$(document).keydown(function (event) {
    if (event.ctrlKey && event.keyCode === 13) {
        document.location = $('#nextApplication').attr("href");
        event.preventDefault();
    }
});

const mapOfDocumentCategory = [
    {
        category: 'IDENTIFICATION',
        subCategories: ['FRENCH_IDENTITY_CARD', 'FRENCH_PASSPORT', 'FRENCH_RESIDENCE_PERMIT', 'DRIVERS_LICENSE', 'FRANCE_IDENTITE', 'OTHER_IDENTIFICATION']
    },
    {
        category: 'RESIDENCY',
        subCategories: ['TENANT', 'OWNER', 'GUEST_PARENTS', 'GUEST', 'GUEST_COMPANY', 'GUEST_ORGANISM', 'SHORT_TERM_RENTAL', 'OTHER_RESIDENCY']
    },
    {
        category: 'PROFESSIONAL',
        subCategories: ['CDI', 'CDI_TRIAL', 'CDD', 'ALTERNATION', 'INTERNSHIP', 'STUDENT', 'PUBLIC', 'CTT', 'RETIRED', 'UNEMPLOYED', 'INDEPENDENT', 'INTERMITTENT', 'STAY_AT_HOME_PARENT', 'NO_ACTIVITY', 'ARTIST', 'OTHER']
    },
    {
        category: 'FINANCIAL',
        subCategories: ['SALARY', 'SCHOLARSHIP', 'SOCIAL_SERVICE', 'RENT', 'PENSION', 'NO_INCOME']
    },
    {
        category: 'TAX',
        subCategories: ['MY_NAME', 'MY_PARENTS', 'LESS_THAN_YEAR', 'OTHER_TAX']
    },
    {
        category: 'IDENTIFICATION_LEGAL_PERSON',
        subCategories: ['LEGAL_PERSON']
    },
    {
        category: 'GUARANTEE_PROVIDER_CERTIFICATE',
        subCategories: ['OTHER_GUARANTEE', 'VISALE']
    }
]

$(document).on("change", "#documentCategory", function () {
    const subCategorySelect = $('#documentSubCategory');
    const selectValue = $(this).val();
    const subCategorySelectedValue = subCategorySelect.val();

    if (subCategorySelectedValue !== 'UNDEFINED' && !isValidSubDocumentCategory(selectValue, subCategorySelectedValue)) {
        subCategorySelect.val("UNDEFINED");
    }

    updateSubDocumentCategoryOptions();

})

function updateSubDocumentCategoryOptions() {
    const categorySelect = $('#documentCategory');
    const subCategorySelect = $('#documentSubCategory');
    const selectedCategory = categorySelect.val();

    subCategorySelect.empty();
    subCategorySelect.append(new Option("Pour tous les documents", "UNDEFINED"));
    const emptyOption = new Option("-------------------", null, false, false);
    emptyOption.disabled = true;
    subCategorySelect.append(emptyOption);

    const category = mapOfDocumentCategory.find(cat => cat.category === selectedCategory);
    const availableSubCategories = [];
    if (category) {
        availableSubCategories.push(...category.subCategories);
    } else {
        for (const item of mapOfDocumentCategory) {
            availableSubCategories.push(...item.subCategories);
        }
    }

    for (const subCat of availableSubCategories) {
        subCategorySelect.append(new Option(subCat, subCat));
    }
}

$(document).on("change", "#documentSubCategory", function () {
    const subCategoryValue = $(this).val();
    const categorySelect = $('#documentCategory');

    const goodCategory = getDocumentCategoryBySubCategory(subCategoryValue);

    if (goodCategory) {
        categorySelect.val(goodCategory);
    }

})

function isValidSubDocumentCategory(documentCategory, subDocumentCategory) {
    const category = mapOfDocumentCategory.find(cat => cat.category === documentCategory);
    if (!category) {
        return false;
    }
    return category.subCategories.includes(subDocumentCategory);
}

function getDocumentCategoryBySubCategory(subCategory) {
    for (const category of mapOfDocumentCategory) {
        if (category.subCategories.includes(subCategory)) {
            return category.category;
        }
    }
    return null;
}