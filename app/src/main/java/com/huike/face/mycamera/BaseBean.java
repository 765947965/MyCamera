package com.huike.face.mycamera;

import java.util.List;

/**
 * @ProjectName: MyCamera
 * @Package: com.huike.face.mycamera
 * @ClassName: BaseBean
 * @Description: java类作用描述
 * @Author: 谢文良
 * @CreateDate: 2020/5/14 9:32
 * @UpdateUser: 更新者
 * @UpdateDate: 2020/5/14 9:32
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class BaseBean {

    private List<Data> list;

    public List<Data> getList() {
        return list;
    }

    public void setList(List<Data> list) {
        this.list = list;
    }

    public static class Data {
        private String name;
        private String feature;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }
    }
}
