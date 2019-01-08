/*
* @providesModule react-native-call-detection
*/
import {
  NativeModules,
  NativeEventEmitter,
  Platform,
  PermissionsAndroid
} from 'react-native';
export const permissionDenied = 'PERMISSION DENIED';

const BatchedBridge = require('react-native/Libraries/BatchedBridge/BatchedBridge');

const NativeCallDetector = NativeModules.CallDetectionManager;
const NativeCallDetectorAndroid = NativeModules.CallDetectionManagerAndroid;

var CallStateUpdateActionModule = require('./CallStateUpdateActionModule');
BatchedBridge.registerCallableModule('CallStateUpdateActionModule', CallStateUpdateActionModule);

const requestPermissionsAndroid = async (permissionMessage) => {
  const checkPermissionPhoneState = PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE);
  const checkPermissionCallLog = PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.READ_CALL_LOG);

  await Promise.all([checkPermissionPhoneState, checkPermissionCallLog])
    .then(async ([gotPermissionPhoneState, gotPermissionCallLog]) => (gotPermissionPhoneState && gotPermissionCallLog)
          ? true
          : await PermissionsAndroid.requestMultiple(
            [PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE, PermissionsAndroid.PERMISSIONS.READ_CALL_LOG],
            // permissionMessage
          ).then((result) => result === PermissionsAndroid.RESULTS.GRANTED)
        )
}

class CallDetectorManager {

    subscription;
    callback
    constructor(callback, readPhoneNumberAndroid = false, permissionDeniedCallback = () => {}, permissionMessage = {
      title: 'Phone State Permission',
      message: 'This app needs access to your phone state in order to react and/or to adapt to incoming calls.'
    }) {
        this.callback = callback
        if (Platform.OS === 'ios') {
            NativeCallDetector && NativeCallDetector.startListener()
            this.subscription = new NativeEventEmitter(NativeCallDetector)
            this.subscription.addListener('PhoneCallStateUpdate', callback);
        }
        else {
            if(NativeCallDetectorAndroid) {
              if(readPhoneNumberAndroid) {
                requestPermissionsAndroid(permissionMessage)
                  .then((permissionGranted) => {
                    if (!permissionGranted) {
                      permissionDeniedCallback(permissionDenied)
                    }
                  })
                  .catch(permissionDeniedCallback)
              }
              NativeCallDetectorAndroid.startListener();
            }
            CallStateUpdateActionModule.callback = callback
        }
    }

    dispose() {
    	NativeCallDetector && NativeCallDetector.stopListener()
    	NativeCallDetectorAndroid && NativeCallDetectorAndroid.stopListener()
        CallStateUpdateActionModule.callback = undefined
      if (this.subscription) {
          this.subscription.removeAllListeners('PhoneCallStateUpdate');
          this.subscription = undefined
      }
    }

    currentCalls(currentCallsCB) {
      if (Platform.OS === 'ios') {
        NativeCallDetector && NativeCallDetector.currentCalls(currentCallsCB);
      } else {
        NativeCallDetectorAndroid && NativeCallDetectorAndroid.currentCallStatus(currentCallsCB);
      }
    }
}
export default module.exports = CallDetectorManager;
