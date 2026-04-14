import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;

class DefaultFirebaseOptions {
  static const FirebaseOptions macos = FirebaseOptions(
    apiKey: 'AIzaSyAWSQosAbTF0JnYLlgIOC7IZdVxn0ht9jc',
    appId: '1:253843381811:ios:2ff066a84d4ba9dfc54d46',
    messagingSenderId: '253843381811',
    projectId: 'hackie-260414-01',
    storageBucket: 'hackie-260414-01.firebasestorage.app',
    databaseURL: 'https://hackie-260414-01-default-rtdb.firebaseio.com',
  );

  static const FirebaseOptions windows = FirebaseOptions(
    apiKey: 'AIzaSyAWSQosAbTF0JnYLlgIOC7IZdVxn0ht9jc',
    appId: '1:253843381811:web:2ff066a84d4ba9dfc54d46',
    messagingSenderId: '253843381811',
    projectId: 'hackie-260414-01',
    storageBucket: 'hackie-260414-01.firebasestorage.app',
    databaseURL: 'https://hackie-260414-01-default-rtdb.firebaseio.com',
  );
  
  static const FirebaseOptions linux = windows;
}
