package com.jun.permission;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Build;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import android.annotation.TargetApi;
import java.lang.annotation.Annotation;
import com.jun.permission.annotation.OnMPermissionDenied;
import com.jun.permission.annotation.OnMPermissionGranted;
import com.jun.permission.annotation.OnMPermissionNeverAskAgain;


/**
 * 权限请求管理类，使用方法如下进行链式调用
 *  ZPermission.with(this)
 *                 .permissions(permissions)
 *                 .requestCode(PERMISSION_REQUEST_CODE)
 *                 .request();
 *
 *  方法具体作用请查看：
 * @see #with(Fragment)
 * @see #with(FragmentActivity)
 * @see #permissions(String[])
 * @see #requestCode(int)
 * @see #request()
 *
 * @author 胡俊华
 */
public class ZPermission {

    /**
     * 日志标记
     */
    private static final String LOG_TAG = "Z_Permission";

    /**
     * Fragment的标志
     */
    private static final String FRAGMENT_TAG = "Z_FRAGMENT";

    /**
     * 请求码，默认为0。可以通过{@link #requestCode(int)}进行设置，
     * 设置之后需要在注解的方法使用同一个请求码，才能得到对应的请求结果。
     * <p>
     * 如果需要修改默认值，需要同步修改{@link OnMPermissionGranted} {@link OnMPermissionDenied} {@link OnMPermissionNeverAskAgain}的默认值
     */
    private int requestCode = 0;

    /**
     * 请求权限
     */
    private String[] permissions;

    /**
     * 请求权限fragment
     */
    private ZPermissionFragment fragment;

    /**
     * Activity或者fragment
     */
    private Object object;


    /**
     * 内部构造方法，如需实例化请使用{@link #with(FragmentActivity)}或者{@link #with(Fragment)}
     */
    private ZPermission(Object object) {
        this.object = object;
        //这里注意，如果为fragment需要使用getChildFragmentManager，而activity使用getSupportFragmentManager
        if (object instanceof Fragment) {
            getZPermissionFragment(((Fragment) object).getChildFragmentManager());
        } else if (object instanceof FragmentActivity) {
            getZPermissionFragment(((FragmentActivity) object).getSupportFragmentManager());
        } else {
            throw new IllegalArgumentException(object.getClass().getName() + " 不支持请求权限！");
        }
    }


    /**
     * 使用FragmentActivity进行实例化
     */
    public static ZPermission with(FragmentActivity activity) {
        return new ZPermission(activity);
    }


    /**
     * 使用Fragment进行实例化
     */
    public static ZPermission with(Fragment fragment) {
        return new ZPermission(fragment);
    }


    /**
     * 设置请求码
     */
    public ZPermission requestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }


    /**
     * 设置请求的权限列表
     */
    public ZPermission permissions(String[] permissions) {
        this.permissions = permissions;
        return this;
    }


    /**
     * 进行权限请求
     */
    public void request() {
        Log.d(LOG_TAG, fragment.getActivityName() + "正在请求权限");
        doRequestPermission(fragment, requestCode, permissions);
    }


    @TargetApi(value = Build.VERSION_CODES.M)
    private void doRequestPermission(ZPermissionFragment fragment, int requestCode, String[] permissions) {
        if (!isOverMarshmallow()) {
            doExecuteSuccess(object, requestCode);
            return;
        }

        //找出需要申请的权限
        List<String> permissionsForRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (isGranted(permission)) {
                Log.d(LOG_TAG, "权限【" + permission + "】已经被授权。");
                continue;
            }
            if (isRevoked(permission)) {
                Log.d(LOG_TAG, "权限【" + permission + "】已经被撤销请求。");
                continue;
            }
            permissionsForRequest.add(permission);
        }
        if (permissionsForRequest.size() > 0) {
            String[] p = new String[permissionsForRequest.size()];
            fragment.requestPermissions(permissionsForRequest.toArray(p), requestCode, this::dispatchResult);
        } else {
            doExecuteSuccess(object, requestCode);
        }

    }


    /**
     * 处理回调结果
     */
    private void dispatchResult(int requestCode, String[] permissions, int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }

        if (deniedPermissions.size() > 0) {
            if (hasNeverAskAgainPermission(deniedPermissions)) {
                doExecuteFailAsNeverAskAgain(object, requestCode);
            } else {
                doExecuteFail(object, requestCode);
            }
        } else {
            doExecuteSuccess(object, requestCode);
        }

    }

    /**
     * 判断权限是否有拒绝询问
     */
    private boolean hasNeverAskAgainPermission(List<String> permissions) {
        if (!isOverMarshmallow()) {
            return false;
        }
        for (String permission : permissions) {
            if (fragment.isNeverAskAgain(permission)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断权限是否已经授权
     */
    private boolean isGranted(@NonNull String permission) {
        return isOverMarshmallow() && fragment.isGranted(permission);
    }


    /**
     * 判断是否在当前包中请求权限，没有的话当做撤销处理
     */
    private boolean isRevoked(@NonNull String permission) {
        return isOverMarshmallow() && fragment.isRevoked(permission);
    }


    /**
     * 判断系统是否为Android6.0以上
     */
    private boolean isOverMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }


    /**
     * 获取请求权限fragment
     */
    private void getZPermissionFragment(@NonNull final FragmentManager fragmentManager) {
        fragment = (ZPermissionFragment) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        //如果为null时，创建新的fragment并添加
        if (fragment == null) {
            fragment = new ZPermissionFragment();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, FRAGMENT_TAG)
                    .commitNow();
        }
    }


    /**
     * 请求权限成功后执行此函数
     */
    private void doExecuteSuccess(Object object, int requestCode) {
        Log.d(LOG_TAG, "权限请求成功");
        executeMethod(object, findMethodWithRequestCode(object.getClass(), OnMPermissionGranted.class, requestCode));
    }


    /**
     * 请求权限拒绝后执行此函数
     */
    private void doExecuteFail(Object object, int requestCode) {
        Log.d(LOG_TAG, "权限请求拒绝");
        executeMethod(object, findMethodWithRequestCode(object.getClass(), OnMPermissionDenied.class, requestCode));
    }


    /**
     * 请求权限拒绝后且不再询问执行此函数
     */
    private void doExecuteFailAsNeverAskAgain(Object object, int requestCode) {
        Log.d(LOG_TAG, "权限请求拒绝且不再询问");
        executeMethod(object, findMethodWithRequestCode(object.getClass(), OnMPermissionNeverAskAgain.class, requestCode));
    }


    /**
     * 通过反射执行方法
     */
    private void executeMethod(Object object, Method executeMethod) {
        if (executeMethod != null) {
            try {
                if (!executeMethod.isAccessible()) {
                    executeMethod.setAccessible(true);
                }
                executeMethod.invoke(object);
            } catch (InvocationTargetException | IllegalAccessException e) {
                Log.e(LOG_TAG, "executeMethod出错：" + e.getMessage());
            }
        }
    }


    /**
     * 找到对应注册的方法
     */
    private <A extends Annotation> Method findMethodWithRequestCode(Class clazz, Class<A> annotation, int requestCode) {
        for (Method method : clazz.getDeclaredMethods()) {
            //寻找是否有该注解的对应方法
            if (method.getAnnotation(annotation) != null &&
                    isEqualRequestCodeFromAnnotation(method, annotation, requestCode)) {
                return method;
            }
        }
        return null;
    }


    /**
     * 通过请求码确定对应函数，请求码通过{@link #requestCode(int)}进行设置。
     *
     * @see #requestCode
     */
    private static boolean isEqualRequestCodeFromAnnotation(Method m, Class clazz, int requestCode) {
        if (clazz.equals(OnMPermissionDenied.class)) {
            return requestCode == m.getAnnotation(OnMPermissionDenied.class).value();
        } else if (clazz.equals(OnMPermissionGranted.class)) {
            return requestCode == m.getAnnotation(OnMPermissionGranted.class).value();
        } else if (clazz.equals(OnMPermissionNeverAskAgain.class)) {
            return requestCode == m.getAnnotation(OnMPermissionNeverAskAgain.class).value();
        } else {
            return false;
        }
    }


}
