angular.module('jahia.saml2')
  .service('maSettingsService', ['$http', 'maContextInfos', function ($http, maContextInfos) {
    this.saveSettings = function (settings) {
      return $http.post(maContextInfos.settingsActionUrl, settings, {
        headers: {'Content-Type': undefined },
        transformRequest: function (data) {
            var formData = new FormData();
            angular.forEach(data, function (value, key) {
                formData.append(key, value);
            });
            return formData;
        }
      });
    };

    this.getSettings = function () {
      return $http.get(maContextInfos.settingsActionUrl);
    };
  }]);
