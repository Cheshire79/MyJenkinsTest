using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;

namespace Sample.Tests
{
	[TestClass]
	public class UnitTest1
	{
		[TestMethod]
		public void TestMethod1()
		{
			string result = " dfg";
			Assert.IsNotNull(result);
		}

		[TestMethod]
		public void TestMethod2()
		{
			string result = null;
			Assert.IsNotNull(result);
		}
	}
}
