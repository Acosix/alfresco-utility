function main()
{
    var managerWidget, suppressAspectsConfigs, suppressedAspects, aspects, idx, aspect, response;

    if (model.widgets)
    {
        managerWidget = model.widgets[0];

        suppressAspectsConfigs = config.scoped.CategoryManager['suppressed-aspects'].getChildren('aspect');
        suppressedAspects = {};
        aspects = {};

        if (suppressAspectsConfigs)
        {
            for (idx = 0; idx < suppressAspectsConfigs.size(); idx++)
            {
                aspect = suppressAspectsConfigs.get(idx).getValue();
                suppressedAspects[aspect] = true;
            }
        }

        response = remote.call('/acosix/api/classificationAspects');
        if (response.status.code === 200)
        {
            response = JSON.parse(response.text);
            for (idx = 0; idx < response.aspects.length; idx++)
            {
                aspect = response.aspects[idx];
                if (aspects[aspect] === undefined)
                {
                    aspects[aspect] = suppressedAspects[aspect] !== true;
                }
            }
        }

        managerWidget.options.classifications = [];
        for (aspect in aspects)
        {
            if (aspects[aspect] === true)
            {
                managerWidget.options.classifications.push(aspect);
            }
        }
    }
}

main();
