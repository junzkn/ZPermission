package com.jun.permission;

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * 专门用于请求权限的无UI的fragment，在ZPermission中使用
 * @see ZPermission
 *
 * @author 胡俊华
 */
public class ZPermissionFragment extends Fragment {


    /**
     * 权限申请结果回调
     */
    private RequestCallBack callBack ;


    /**
     * 权限是否已授权
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isGranted(String permission) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("ZPermissionFragment没有绑定Activity，请检查！");
        }
        return fragmentActivity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * 权限是否已撤销申请
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isRevoked(String permission) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("ZPermissionFragment没有绑定Activity，请检查！");
        }
        return fragmentActivity.getPackageManager().isPermissionRevokedByPolicy(permission, fragmentActivity.getPackageName());

    }

    /**
     * 权限是否已拒绝且不再询问
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isNeverAskAgain(String permission) {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("ZPermissionFragment没有绑定Activity，请检查！");
        }
        return fragmentActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED &&
                !fragmentActivity.shouldShowRequestPermissionRationale(permission);
    }

    /**
     * 获取当前fragment对应activity的名称
     */
    public String getActivityName() {
        FragmentActivity fragmentActivity = getActivity();
        if (fragmentActivity == null) {
            throw new IllegalStateException("ZPermissionFragment没有绑定Activity，请检查！");
        }
        String activityName = fragmentActivity.toString();
        return activityName.substring(activityName.lastIndexOf(".")+1, activityName.indexOf("@"));
    }

    /**
     * 请求权限
     */
    public void requestPermissions(String[] permissions, int requestCode,RequestCallBack callBack){
        this.callBack = callBack ;
        this.requestPermissions(permissions,requestCode);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(callBack!=null){
            callBack.onRequestPermissionsResult(requestCode,permissions,grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * 权限申请结果回调接口
     */
    public interface RequestCallBack {
        /**
         * 请求权限回调
         *
         * @param requestCode 请求码
         * @param permissions 申请的权限
         * @param grantResults 对应的权限申请结果
         */
        void onRequestPermissionsResult(int requestCode,String[] permissions, int[] grantResults);
    }

}
