package cool.scx.http.x.test;

import cool.scx.http.x.http1.body_supplier.HttpChunkedByteSupplier;
import cool.scx.http.x.http1.body_supplier.HttpChunkedParseException;
import cool.scx.io.DefaultByteInput;
import cool.scx.io.exception.ScxIOException;
import cool.scx.io.supplier.ByteArrayByteSupplier;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HttpChunkedByteSupplierTest {

    public static void main(String[] args) throws ScxIOException {
        testGet();
        testFail();
        testFail2();
        testFail3();
    }

    @Test
    public static void testGet() throws ScxIOException {
        // 模拟的复杂分块数据
        byte[] mockData = ("4\r\nWiki\r\n3\r\nHel\r\n7\r\nloWorld\r\n0\r\n\r\n").getBytes();
        var byteArrayByteReader = new DefaultByteInput(new ByteArrayByteSupplier(mockData));

        // 创建 HttpChunkedDataSupplier 实例
        HttpChunkedByteSupplier supplier = new HttpChunkedByteSupplier(byteArrayByteReader);

        var byteInput = new DefaultByteInput(supplier);
        // 验证数据块
        var dataNode1 = byteInput.readAll();
        Assert.assertEquals(new String(dataNode1), "WikiHelloWorld");

    }

    @Test
    public static void testFail() throws ScxIOException {
        // 模拟的复杂分块数据
        byte[] mockData = ("4\r\nWiki\r\n3\r\nHel\r\n7\r\nloWorld\r\n0\r\n\r").getBytes();
        DefaultByteInput byteArrayByteReader = new DefaultByteInput(new ByteArrayByteSupplier(mockData));

        // 创建 HttpChunkedDataSupplier 实例
        HttpChunkedByteSupplier supplier = new HttpChunkedByteSupplier(byteArrayByteReader);

        var byteInput = new DefaultByteInput(supplier);
        // 验证错误
        try {
            byteInput.readAll();
        } catch (Throwable e) {
            Assert.assertEquals(e.getClass(), HttpChunkedParseException.class);
            Assert.assertEquals(e.getMessage(), "错误的终结分块, 终结块不完整: 缺少 \\r\\n !!!");
            return;
        }
        Assert.fail("应该发生错误, 但没有发生错误!!!");

    }

    @Test
    public static void testFail2() throws ScxIOException {
        // 模拟的复杂分块数据
        byte[] mockData = ("4\r\nWiki\r\n3\r\nHel\r\n7\r\nloWorld\r\n0\r\n???\r\n").getBytes();
        DefaultByteInput byteArrayByteReader = new DefaultByteInput(new ByteArrayByteSupplier(mockData));

        // 创建 HttpChunkedDataSupplier 实例
        HttpChunkedByteSupplier supplier = new HttpChunkedByteSupplier(byteArrayByteReader);

        var byteInput = new DefaultByteInput(supplier);
        // 验证错误
        try {
            byteInput.readAll();
        } catch (Throwable e) {
            Assert.assertEquals(e.getClass(), HttpChunkedParseException.class);
            Assert.assertEquals(e.getMessage(), "错误的终结分块, 应为空块但发现了内容 !!!");
            return;
        }
        Assert.fail("应该发生错误, 但没有发生错误!!!");

    }

    @Test
    public static void testFail3() throws ScxIOException {
        // 模拟的复杂分块数据
        byte[] mockData = ("4\r\nWiki\r\n?\r\nHel\r\n7\r\nloWorld\r\n0\r\n\r\n").getBytes();
        DefaultByteInput byteArrayByteReader = new DefaultByteInput(new ByteArrayByteSupplier(mockData));

        // 创建 HttpChunkedDataSupplier 实例
        HttpChunkedByteSupplier supplier = new HttpChunkedByteSupplier(byteArrayByteReader);

        var byteInput = new DefaultByteInput(supplier);
        // 验证错误
        try {
            byteInput.readAll();
        } catch (Throwable e) {
            Assert.assertEquals(e.getClass(), HttpChunkedParseException.class);
            Assert.assertEquals(e.getMessage(), "错误的分块长度 : ?");
            return;
        }
        Assert.fail("应该发生错误, 但没有发生错误!!!");

    }

}
