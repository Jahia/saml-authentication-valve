angular.module('JahiaOAuthApp')
  .service('settingsService', ['$http', '$routeParams', 'maContextInfos', function ($http, $routeParams, maContextInfos) {
    $routeParams.connectorServiceName='Saml'
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

    this.getMapperMapping = function(data) {
        return $http.get(maContextInfos.manageMappersActionUrl + "?action=getMapperMapping&mapperServiceName="+data.mapperServiceName);
    }

    this.setMapperMapping = function(data) {
        console.log('setMapperMapping',data);
        var p = {
            success: function(f) {
                return p;
            },
            error: function(f) {
                return p;
            }
        }
        return p;
    }

    this.getConnectorProperties = function(data) {
        return $http.get(maContextInfos.manageMappersActionUrl + "?action=getConnectorProperties");
    }

    this.getMapperProperties = function(data) {
        return $http.get(maContextInfos.manageMappersActionUrl + "?action=getMapperProperties&mapperServiceName="+data.mapperServiceName);
    }

  }]);
