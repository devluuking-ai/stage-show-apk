#include <jni.h>
#include <string>
#include <cstdlib>
#include "node.h"

// nodejs-mobile bridge: node::Start blocks until the Node event loop ends,
// so Kotlin calls this on a dedicated background thread, once per process.
extern "C" JNIEXPORT jint JNICALL
Java_com_devluuking_stageshow_NodeBridge_startNodeWithArguments(
        JNIEnv *env, jobject /*thiz*/, jobjectArray arguments) {
    jsize argc = env->GetArrayLength(arguments);
    char **argv = new char *[argc + 1];
    for (int i = 0; i < argc; i++) {
        jstring js = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *utf = env->GetStringUTFChars(js, nullptr);
        argv[i] = strdup(utf);
        env->ReleaseStringUTFChars(js, utf);
        env->DeleteLocalRef(js);
    }
    argv[argc] = nullptr;
    int exitCode = node::Start(argc, argv);
    for (int i = 0; i < argc; i++) free(argv[i]);
    delete[] argv;
    return exitCode;
}
