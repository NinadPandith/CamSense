package androidx.activity.result.contract
abstract class ActivityResultContract<I, O>
class ActivityResultContracts {
    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>()
    class RequestPermission : ActivityResultContract<String, Boolean>()
}
